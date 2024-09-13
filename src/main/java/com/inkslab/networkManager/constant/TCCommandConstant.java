package com.inkslab.networkManager.constant;

public interface TCCommandConstant {
    /*
     *查看所有的队列
     */
    String LS_QDISC = "sudo tc -s qdisc ls dev ";

    /**
     * 查看所有的分类
     */
    String LS_CLASS = "sudo tc -s class ls dev ";

    /**
     * 创建跟序列命令前后缀，中间拼接一个网卡
     */
    String CREATE_ROOT_QDISC_PRE = "sudo tc qdisc add dev ";
    String CREATE_ROOT_QDISC_SUF = " root handle 1: htb default 1";

    /**
     * 创建主分类命令前缀和后缀
     */
    String CREATE_MAIN_CLASS_PRE = "sudo tc class add dev ";
    String CREATE_MAIN_CLASS_SUF = " parent 1:0 classid 1:1 htb rate ";

    /**
     * 创建子分类命令前缀和后缀
     */
    String CREATE_SON_CLASS_PRE = "sudo tc class add dev ";
    String CREATE_SON_CLASS_SUF = " parent 1:0 classid 1:10 htb rate ";

    /**
     * 创建过滤器
     */
    String CREATE_FILTER_PRE = "sudo tc filter add dev ";
    String CREATE_FILTER_SUF = " parent 1:0 protocol ip prio 100 route to 2 flowid 1:10";

    /**
     * 创建路由
     */
    String CREATE_ROUTE_PRE  = "sudo ip route add ";
    String CREATE_ROUTE_MID1 = " dev ";
    String CREATE_ROUTE_MID2 = " via ";
    String CREATE_ROUTE_SUF  = " realm 2";


    /**
     * 删除控制
     *  sudo tc qdisc del dev <server_adapter:enp96s0f1> root
     *  sudo ip route del <target_ip:192.168.1.171>
     */
    String DELETE_QDISC_PRE =  "sudo tc qdisc del dev ";
    String DELETE_QDISC_SUF = " root";

    /**
     * 删除路由命令
     */
    String DELETE_ROUTE_PRE = "sudo ip route del ";



    /**
     * 展示路由表信息
     */
    String SHOW_ROUTE_INFO = "sudo ip route show";


    /**
     * 展示过滤器命令
     */
    String SHOW_FILTER_INFO = "sudo tc filter show dev ";
}
