package com.inkslab.networkManager.model.dto.bandwidthControl;

import lombok.Data;

/**
 * 配置网络请求类
 */
@Data
public class SetControlRequest {

    /**
     * 源服务器
     */
    private String local;

    /**
     * 目标服务器
     */
    private String target;

    /**
     * 要配置的带宽
     */
    private String bandwidth;

    /**
     * 延迟
     */
    private String delay;
}
