package com.inkslab.networkManager.model.dto;

import lombok.Data;

@Data
public class ExecuteMessage {
    /**
     * 退出码
     */
    private Integer exitNum;
    /**
     * 正常输出信息
     */
    private String normalMessage;
    /**
     * 错误输出信息
     */
    private String errorMessage;
}
