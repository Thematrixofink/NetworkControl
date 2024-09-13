package com.inkslab.networkManager.model.entity;

import lombok.Data;

@Data
public class Route {
    //192.168.1.173 via 192.168.1.104 dev enp96s0f1 realm 4

    /**
     * 目标IP
     */
    String targetIp;

    /**
     * 本机IP
     */
    String localIp;

    /**
     * 网卡
     */
    String nicName;

    /**
     * realm
     */
    Integer realm;
}
