package com.inkslab.networkManager.config;
import com.inkslab.networkManager.model.entity.Server;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务器集群配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cluster")
public class ClusterConfig {
    //默认带宽
    private String defaultBandwidth;
    //服务器集群
    private List<Server> servers;
    //用户名
    private String username;
    //密码
    private String password;

    //servers的信息，便于操作
    //Key 为服务器的IP，Value为服务器的信息
    private Map<String,Server> clusterInfo;

    @PostConstruct
    public void init() {
        clusterInfo = new HashMap<>();
        for (Server server : servers) {
            clusterInfo.put(server.getIp(), server);
        }
    }
}

