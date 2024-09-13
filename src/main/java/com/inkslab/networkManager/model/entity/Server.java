package com.inkslab.networkManager.model.entity;

import lombok.Data;

/**
 * 服务器实体类
 */
@Data
public class Server {
    /**
     * 服务器名称
     */
    private String name;
    /**
     * 服务器IP
     */
    private String ip;
    /**
     * 服务器端口
     */
    private int port;
    /**
     * 网卡名称
     */
    private String nicName;
}
