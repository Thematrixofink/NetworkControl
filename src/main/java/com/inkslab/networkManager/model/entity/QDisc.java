package com.inkslab.networkManager.model.entity;

import lombok.Data;

@Data
public class QDisc {
    /**
     * 队列的类别
     */
    String type;

    /**
     * 队列的名称(handle)
     */
    String name;

    /**
     * 父类是什么
     */
    String parent;

    /**
     * 延迟
     */
    String delay;

}
