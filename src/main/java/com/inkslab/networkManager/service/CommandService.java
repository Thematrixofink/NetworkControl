package com.inkslab.networkManager.service;

import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 执行命令Service
 */
public interface CommandService {

    /**
     * 执行命令,并获取到输出结果
     * @param command
     * @return
     */
    String execShell(String ip,String command);

    /**
     * 执行多个shell命令，并拿到输出
     * @param ip
     * @param commands
     * @return
     */
    String execShells(String ip, List<String> commands);

    /**
     * 蝴蝶创建根序列的命令
     * @param nicName
     * @return
     */
    String getCreateRootQdiscCommand(String nicName);

    /**
     * 获取创建主分类的命令
     * @param nicName
     * @return
     */
    String getCreateMainClassCommand(String nicName);

    /**
     * 获取创建子分类的命令
     * @param nicName 网卡名称
     * @param bandWidth 带宽
     * @return
     */
    String getCreateSonClassCommand(String nicName,String parentName,String sonClassNum,String bandWidth);

    /**
     * 获取创建过滤器的命令
     * @param nicName 网卡的名称
     * @return
     */
    String getCreateFilterCommand(String nicName,String mainClassNum);

    /**
     * 获取创建路由命令
     * @param target 目标
     * @param nicName 源服务器的网卡名称
     * @param local 源服务器的IP地址
     * @return
     */
    String getCreateRouteCommand(String target,String nicName,String local,String mainClassNum);

    /**
     * 获取删除队列命令
     * @param localNicName
     * @return
     */
    String getDeleteQDISCCommand(String localNicName);

    /**
     * 获取删除路由命令
     * @param targetIp
     * @return
     */
    String getDeleteRouteCommand(String targetIp);

    /**
     * 获取删除分类命令
     * @param nicName
     * @param realm
     * @return
     */
    String getDeleteClassCommand(String nicName,Integer realm);

    /**
     * 获取删除过滤器命令
     * @param localNicName
     * @param pref
     * @param handle
     * @return
     */
    String getDeleteFilterCommand(String localNicName, String pref, String handle);

    /**
     * 获取设置延迟的命令
     * @param localNicName
     * @param delay
     * @param sonClassNum
     * @param delayQdiscNum
     * @return
     */
    String getSetDelayCommend(String localNicName,String delay,String sonClassNum,String delayQdiscNum);

    /**
     * 获取删除延迟命令
     * @param localIp
     * @param targetIp
     * @return
     */
    String getDelDelayCommand(String localIp,String targetIp);
}
