package com.inkslab.networkManager.model.entity;

import io.swagger.models.auth.In;
import lombok.Data;

@Data
public class Filter {
    //filter protocol ip pref 100 route chain 0
    //filter protocol ip pref 100 route chain 0 fh 0xffff0002 flowid 1:2 to 2
    //filter protocol ip pref 100 route chain 0 fh 0xffff0003 flowid 1:3 to 3
    //filter protocol ip pref 100 route chain 0 fh 0xffff0004 flowid 1:4 to 4
    String pref;
    String chain;
    String handle;
    String flowid;
    Integer realm;
}
