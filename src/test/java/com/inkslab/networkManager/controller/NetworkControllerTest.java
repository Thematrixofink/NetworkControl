package com.inkslab.networkManager.controller;

import com.inkslab.networkManager.config.ClusterConfig;
import com.inkslab.networkManager.constant.TCCommandConstant;
import com.inkslab.networkManager.model.entity.*;
import com.inkslab.networkManager.service.CommandService;
import com.inkslab.networkManager.service.NetworkService;
import com.inkslab.networkManager.service.impl.NetworkServiceImpl;
import com.inkslab.networkManager.utils.NetUtils;
import com.inkslab.networkManager.utils.StreamConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

@SpringBootTest
@Slf4j
class NetworkControllerTest {

    @Resource
    private ClusterConfig config;
    @Resource
    private NetworkService networkService;
    @Resource
    private CommandService commandService;


    /**
     * 测试运行ipconfig命令
     */
    @Test
    void getAllFile() {
            Runtime runtime = Runtime.getRuntime();
            try {
                Process process = runtime.exec("ipconfig");
                BufferedReader reader = StreamConvertUtil.toBufferReader(process.getInputStream());
                StringBuilder result = new StringBuilder();
                String temp = null;
                while((temp = reader.readLine()) != null){
                    result = result.append(temp);
                    result.append("\n");
                }
                process.waitFor();
                System.out.println(result.toString());;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
    }

    /**
     * 测试从配置文件获取服务器集群信息
     */
    @Test
    void getServersConfig(){
        System.out.println();
    }

    @Test
    void testExecCommand(){
        String ifconfig = commandService.execShell("10.1.1.104", "ifconfi");
        System.out.println(ifconfig);
    }

    @Test
    void testGetClass(){
        String result = commandService.execShell("49.232.241.237", TCCommandConstant.LS_CLASS+"eth");
        System.out.println(result);
    }

    @Test
    void testExecSHells(){
        List<String> commands = new ArrayList<>();
        commands.add("pwd");
        commands.add("ls");
        commands.add("ifconfig");
        System.out.println(commandService.execShells("10.1.1.104", commands));
    }

    @Test
    void setBandwidth() {
        NetworkServiceImpl networkService1 = new NetworkServiceImpl();
//        String local = networkConfigRequest.getLocal();
//        String target = networkConfigRequest.getTarget();
//        String bandwidth = networkConfigRequest.getBandwidth();
        String localIp = "10.1.1.104";
        String target = "192.168.1.172";
        String bandwidth = "300mbit";
        String localNicName = "enp96s0f1";
        List<String> commands = new ArrayList<>();

        //1.获取创建根序列命令
        //sudo tc qdisc add dev <nicname> root handle 1:0 htb default 1
        String createRootCommand = commandService.getCreateRootQdiscCommand(localNicName);
        log.info(createRootCommand);
        commands.add(createRootCommand);

        //2.获取创建主分类命令
        //sudo tc class add dev <server_adapter:enp96s0f1> parent 1:0 classid 1:1 htb rate 1000mbit
        String createMainClassCommand = commandService.getCreateMainClassCommand(localNicName);
        log.info(createMainClassCommand);
        commands.add(createMainClassCommand);

        //获取最大新的子分类序号
        List<Clazz> classInfo = networkService.getClassInfo(localIp);
        String newClassNum = networkService1.getMaxSonClassNum(classInfo);
        log.info("当前可用的最大的子集合编号为:"+newClassNum);
//        Optional<Clazz> max = classInfo.stream().max(new Comparator<Clazz>() {
//            @Override
//            public int compare(Clazz o1, Clazz o2) {
//                String[] split1 = o1.getName().split(":");
//                String[] split2 = o2.getName().split(":");
//                return Integer.parseInt(split1[1]) - Integer.parseInt(split2[1]);
//            }
//        });
//        String newClassNum = null;
//        String mainClassNum = null;
//        if(max.isEmpty()){
//            mainClassNum = "1";
//            newClassNum = "2";
//        }else {
//            String name = max.get().getName();
//            String[] split = name.split(":");
//           // mainClassNum = split[1];
//            newClassNum = String.valueOf(Integer.parseInt(mainClassNum) + 1);
//        }

        //3.创建子分类命令
        //sudo tc class add dev <server_adapter:enp96s0f1> parent 1:1 classid 1:mainClassNum htb rate 100mbit
        String createSonClassCommand = commandService.getCreateSonClassCommand(localNicName,"1",newClassNum,bandwidth);
        log.info(createSonClassCommand);
        commands.add(createSonClassCommand);

        //4.拼接创建过滤器命令
        //tc filter add dev eth0 parent 1:0 protocol ip prio 100 route to mainClassNum flowid 1:mainClassNum
        String createFilterCommand = commandService.getCreateFilterCommand(localNicName,newClassNum);
        log.info(createFilterCommand);
        commands.add(createFilterCommand);

        //5.拼接创建路由命令
        //todo 实际运行环境要改为本地的ip（内网）
        String createRouteCommand = commandService.getCreateRouteCommand(target,localNicName,"192.168.1.104",newClassNum);
        log.info(createRouteCommand);
        commands.add(createRouteCommand);
        //6.执行命令
        String result = commandService.execShells(localIp, commands);

        System.out.println(result);

        //todo 对输出结果进行处理，判断是否创建成功
    }

    @Test
    void testSetBandWidth(){
        NetworkServiceImpl networkService1 = new NetworkServiceImpl();
        String in = "class htb 1:11 parent 1:10 prio 0 rate 100Mbit ceil 100Mbit burst 1600b cburst 1600b \n" +
                " Sent 0 bytes 0 pkt (dropped 0, overlimits 0 requeues 0) \n" +
                " backlog 0b 0p requeues 0 \n" +
                " lended: 0 borrowed: 0 giants: 0\n" +
                " tokens: 2000 ctokens: 2000\n" +
                "\n" +
                "class htb 1:10 root rate 100Mbit ceil 100Mbit burst 1600b cburst 1600b \n" +
                " Sent 0 bytes 0 pkt (dropped 0, overlimits 0 requeues 0) \n" +
                " backlog 0b 0p requeues 0 \n" +
                " lended: 0 borrowed: 0 giants: 0\n" +
                " tokens: 2000 ctokens: 2000\n" +
                "\n" +
                "class htb 1:1234 root prio 0 rate 1Gbit ceil 1Gbit burst 1375b cburst 1375b \n" +
                " Sent 0 bytes 0 pkt (dropped 0, overlimits 0 requeues 0) \n" +
                " backlog 0b 0p requeues 0 \n" +
                " lended: 0 borrowed: 0 giants: 0\n" +
                " tokens: 187 ctokens: 187\n";
        List<Clazz> clazzKeyInfo = networkService1.getClazzKeyInfo(in);
        Optional<Clazz> max = clazzKeyInfo.stream().max(new Comparator<Clazz>() {
            @Override
            public int compare(Clazz o1, Clazz o2) {
                String[] split1 = o1.getName().split(":");
                String[] split2 = o2.getName().split(":");
                return Integer.parseInt(split1[1]) - Integer.parseInt(split2[1]);
            }
        });
        String name = max.get().getName();
        System.out.println();
    }

    @Test
    void setDeleteControl(){

    }

    @Test
    void testGetRouteInfo(){
        List<Route> routeInfo = networkService.getRouteInfo("10.1.1.104");
        System.out.println();
    }

    @Test
    void testDelRoot(){
        QDisc qDiscInfo = networkService.getQDiscInfo("10.1.1.104");
        System.out.println();
        networkService.deleteRoot("10.1.1.104");
        QDisc qDiscInfo1 = networkService.getQDiscInfo("10.1.1.104");
        System.out.println();
    }

    @Test
    void testDelControl(){
        networkService.deleteControl("10.1.1.104","192.168.1.171");
        networkService.deleteControl("10.1.1.104","192.168.1.172");
        networkService.deleteControl("10.1.1.104","192.168.1.173");
    }

    @Test
    void testGetControl(){
        List<Control> controlInfo = networkService.getControlInfo("100.105.103.116");
        System.out.println();
    }

    @Test
    void testGetFilterInfo(){
        List<Filter> filterInfo = networkService.getFilterInfo("10.1.1.104");
        System.out.println();
    }

    @Test
    void testValidIp(){
        Assertions.assertEquals(true, NetUtils.validIp("192.168.1.1"));
        Assertions.assertEquals(true,NetUtils.validIp("192.168.1.104"));
        Assertions.assertEquals(true,NetUtils.validIp("192.168.1.172"));
        Assertions.assertEquals(false,NetUtils.validIp("192.168.2334.104"));
        Assertions.assertEquals(false,NetUtils.validIp("192.168.1.256"));
        Assertions.assertEquals(false,NetUtils.validIp("168.1.256"));

    }

    @Test
    void testValidBandwidth(){
        NetworkController controller = new NetworkController();
        Assertions.assertEquals(true,NetUtils.validBandWidth("100mbit"));
        Assertions.assertEquals(true,NetUtils.validBandWidth("100kbit"));
        Assertions.assertEquals(true,NetUtils.validBandWidth("100gbit"));
        Assertions.assertEquals(false,NetUtils.validBandWidth("kbit"));
        Assertions.assertEquals(false,NetUtils.validBandWidth("100hbit"));
        Assertions.assertEquals(false,NetUtils.validBandWidth("100kbits"));
    }

    @Test
    void getAllInfo(){
        List<Control> allInfo = networkService.getAllInfo(config.getServers());
        System.out.println();
    }


}
