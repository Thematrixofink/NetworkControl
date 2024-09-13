package com.inkslab.networkManager.service;

import com.inkslab.networkManager.model.dto.bandwidthControl.SetControlRequest;
import com.inkslab.networkManager.model.entity.*;

import java.util.List;


/**
 * 网络控制Service
 */
public interface NetworkService {

    /**
     * 获取集群信息
     * @return 集群信息
     */
    List<Server> getClusterInfo();

    /**
     * 获取默认带宽
     * @return
     */
    String getDefaultBandwidth();

    /**
     * 设置带宽
     * @param setControlRequest
     * @return
     */
    String setBandwidth(SetControlRequest setControlRequest);

    /**
     * 删除控制
     * @param localIp
     * @param targetIp
     */
    String deleteControl(String localIp, String targetIp);

    /**
     * 获取根序列QDsic命令
     * @param ip
     * @return
     */
    List<QDisc> getQDiscInfo(String ip);

    /**
     * 获取分类信息
     * @param ip
     * @return
     */
    List<Clazz> getClassInfo(String ip);

    /**
     * 获取路由信息
     * @param ip
     * @return
     */
    List<Route> getRouteInfo(String ip);

    /**
     * 获取过滤器信息
     * @param ip
     * @return
     */
    List<Filter> getFilterInfo(String ip);

    /**
     * 删除根序列
     * @param ip
     * @return
     */
    String deleteRoot(String ip);


    /**
     * 查询控制信息
     * @param ip
     * @return
     */
    List<Control> getControlInfo(String ip);

    List<Control> getAllInfo(List<Server> servers);

}
