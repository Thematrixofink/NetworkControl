package com.inkslab.networkManager.model.entity;

import lombok.Data;

@Data
public class Clazz {
    /**
     * class的类型
     */
    String type;

    /**
     * class的名称
     */
    String name;

    /**
     * 该类最低获得的带宽
     */
    String rate;

    /**
     * 该分类最大得到的带宽
     */
    String ceil;

    /**
     * 父类
     */
    String father;
}
