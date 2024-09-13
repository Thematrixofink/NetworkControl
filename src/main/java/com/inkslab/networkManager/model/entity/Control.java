package com.inkslab.networkManager.model.entity;

import lombok.Data;

@Data
public class Control {
    /**
     * 本地IP地址
     */
    String localIp;
    /**
     * 目的IP地址
     */
    String targetIp;
    /**
     * 带宽
     */
    String bandWidth;

    /**
     * 默认带宽
     */
    String defaultBandWidth;

    /**
     * 延迟
     */
    String delay;
}
