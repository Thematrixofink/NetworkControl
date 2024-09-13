package com.inkslab.networkManager.model.dto.bandwidthControl;

import lombok.Data;

/**
 * 删除控制的请求类
 */
@Data
public class DeleteControlRequest {
    String localIp;
    String targetIp;
}
