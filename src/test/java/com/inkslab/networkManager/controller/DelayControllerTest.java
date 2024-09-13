package com.inkslab.networkManager.controller;

import com.inkslab.networkManager.common.BaseResponse;
import com.inkslab.networkManager.utils.NetUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class DelayControllerTest {

    @Test
    void setDelay() {

    }

    @Test
    void testValidDelay(){
        assertTrue(NetUtils.validDelay("0ms"));
        assertTrue(NetUtils.validDelay("1ms"));
        assertTrue(NetUtils.validDelay("100s"));
        assertTrue(!NetUtils.validDelay("10ns"));
        assertTrue(!NetUtils.validDelay("asdas"));
        assertTrue(!NetUtils.validDelay("s"));
        assertTrue(!NetUtils.validDelay("ns"));
    }
}
