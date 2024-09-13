# TC命令学习

### 必要知识

​		Linux中的流量控制器TC(Traffic Control) 是用于Linux内核的流量控制。主要是通过在网络输出端口处建立一个队列来实现流量控制。



**流量处理由三组对象控制：**

1. `qdisc`（排队规则）

   内核如果需要通过某个网络接口发送数据包，他都需要按照为这个接口配置的qdisc把数据包加入队列。然后，内核会尽可能多的从qdisc中取数据包，把他们交给网络适配器驱动模块。

   最简单的qdisc就是pfifo，不对数据包进行任何的处理，数据包采用先入先出的方式通过队列。

   **这里我们采用的为：`HTB`**

   > HTB是Hierarchy Token  Bucket的缩写。它实现了一个丰富的连接共享类别体系。使用**HTB可以很容易地保证每个类别的带宽，它也允许特定的类可以突破带宽上限，占用别的类的带宽**。HTB可以通过TBF(Token Bucket Filter)实现带宽限制，也能够划分类别的优先级。

2. `class `（类别）

   某些**QDisc可以包含一些类别，不用的类别中可以采取不同的策略**。

   通过这些细分的QDisc哈可以为进入队列的数据包排队（比如先进入什么快速的类别，再进入慢速的类别，这里我们不会用到这个功能）

3. `filter`（过滤器）

   filter用于为数据包进行分类，决定它们按照何种QDisc进入队列。无论何时，一个数据包进入一个划分了子类的类别中，都需要进行分类。使用filter进行分类时，内核会调用附属于这个类的所有过滤器，直到返回一个判决。



##### 命名规则

所有的QDsic、类、过滤器都有ID，ID可以手工设置，也可以由内核自动分配。**ID由一个主序列号和一个从序列号组成，两个数字用一个冒号分开。**

- QDisc：一个Qdisc会被分配一个**主序列号，叫做句柄handle**，然后把从序列号作为类的命名空间。习惯上，需要为有子类的QDsic显示分配一个句柄。
- Class：  在同一个QDisc里面的类分享这个QDisc的主序列号，但是每个类都有自己的从序列号，叫做类识别符(classid)。类识别符只与父QDisc有关，和父类无关。类的命名习惯和QDisc的相同。
- Filter：



### 控制思路

**采用的策略为：只创建一个主类别，主类别下面创建多个子类别，为各个子类别限制特定大小的带宽，并为每个子类别绑定一个过滤器。最后在路由表中加入相关条目，使得从本机到特定机器的分组进入我们的子类。**

```shell
1.针对网络物理设备（如以太网卡eth0）绑定一个队列QDisc；

2.在该队列上建立一个主分类class；

3.在主分类下建立子分类，进行特定带宽控制，并为每一分类建立一个基于路由的过滤器filter；

4.最后与过滤器相配合，建立特定的路由表
```

##### 1.查看队列、分类

```shell
#查看现有的QDisc
#eth0 为网卡的名称
tc -s qdisc ls dev eth0

#输出为：
qdisc htb 1: root refcnt 3 r2q 10 default 1 direct_packets_stat 1629542 direct_qlen 1000
 Sent 229632116 bytes 1629542 pkt (dropped 0, overlimits 0 requeues 0) 
 backlog 0b 0p requeues 0 
 
#解释：
qdisc的类型为htb
句柄为1：
refcnt 3 表示有三个引用指向此
default 1：默认类别为 1，未明确分类的流量将被分配到类 1
```



```shell
#查看现有的分类
tc -s class ls dev eth0
#示例输出
class htb 1:10 root prio 0 rate 100Mbit ceil 100Mbit burst 1600b cburst 1600b 
 Sent 0 bytes 0 pkt (dropped 0, overlimits 0 requeues 0) 
 backlog 0b 0p requeues 0 
 lended: 0 borrowed: 0 giants: 0
 tokens: 2000 ctokens: 2000

class htb 1:1 root prio 0 rate 1Gbit ceil 1Gbit burst 1375b cburst 1375b 
 Sent 1139853728 bytes 8088310 pkt (dropped 0, overlimits 0 requeues 0) 
 backlog 0b 0p requeues 0 
 lended: 8088310 borrowed: 0 giants: 0
 tokens: 173 ctokens: 173

#解释：
有两个分类，都是htb类型，他们的命名分别为1:10、1:1
第一个类最少获得带宽rate为100Mbit，最多获得带宽ceil为100Mbit，第二个类同理。
```

##### 2.创建根序列

```shell
#创建一个句柄为1:0的根序列，类型为htb
sudo tc qdisc add dev <nicname> root handle 1:0 htb default 1
sudo tc qdisc add dev eth0 root handle 1:0 htb default 1
```

##### 3.创建主类

```shell
#在1:0根序列下面创建一个分类，句柄为1:0,rate表示该分类最少获得1000mbit（默认带宽）
sudo tc class add dev eth0 parent 1:0 classid 1:1 htb rate 1000mbit
```

##### 4.创建子分类

```shell
#在1:1分类下面创建一个子分类1:2，其最少获得100mbit，最大获得100mbit带宽
sudo tc class add dev eth0 parent 1:1 classid 1:2 htb rate 100mbit ceil 100mbit
```

##### 5.创建过滤器

```shell
#在父类1:0下创建一个过滤器，通过路径（route to） x 绑定到 1:x 子分类处
#prio为优先级
#过滤器需要在主分类下创建，之后绑定到子分类上
tc filter add dev eth0 parent 1:0 protocol ip prio 100 route to 2 flowid 1:2
tc filter add dev eth0 parent 1:0 protocol ip prio 100 route to 3 flowid 1:3
tc filter add dev eth0 parent 1:0 protocol ip prio 100 route to 4 flowid 1:4
```

##### 6.创建路由

```shell
#发往192.168.1.24的数据包通过路径2进行转发
ip route add 192.168.1.24 dev eth0 via 192.168.1.66 realm 2
```

总流程：发往192.168.1.24的数据包通过路径2进行转发，同时路径2通过Filter和1:2子分类相关联，又因为1:2子分类是限制带宽100mbit，因此就实现了对带宽的限制。

##### 5.删除控制

```shell
#删除根序列，会删除所有分类
sudo tc qdisc del dev enp96s0f1 root
#删除子分类，可以做到删除单个控制，而不影响其他控制
sudo tc class del dev enp96s0f1 classid 1:2
#删除路由表中相关条目
sudo ip route del <target_ip:192.168.1.171>
sudo ip route del 10.244.240.13
sudo ip route del 192.168.1.171
```

##### 测试带宽


