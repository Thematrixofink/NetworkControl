package com.inkslab.networkManager.utils;


import com.inkslab.networkManager.model.dto.ExecuteMessage;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
public class ProcessUtils {

    /**
     * 获取运行的控制台信息
     * @return
     */
    public static ExecuteMessage getRunProcessMessage(Process process) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            //得到退出的状态码
            int exitNum = process.waitFor();
            executeMessage.setExitNum(exitNum);
            if (exitNum == 0) {
                //成功
                log.info("进程正常退出~");
                //获取进程的输出
                //把process的东西输入到一个输入流里面，然后打印，所以是输入流！！！
                BufferedReader normalReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                ;
                String compileLineMessage;
                StringBuilder normalMessageBuilder = new StringBuilder();
                while ((compileLineMessage = normalReader.readLine()) != null) {
                    normalMessageBuilder.append(compileLineMessage);
                }
                executeMessage.setNormalMessage(normalMessageBuilder.toString());
                normalReader.close();
            } else {
                //失败
                log.error("进程错误退出,状态码:" + exitNum);
                //获取进程的输出
                BufferedReader normalReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String compileLineMessage;
                StringBuilder normalMessageBuilder = new StringBuilder();
                while ((compileLineMessage = normalReader.readLine()) != null) {
                    normalMessageBuilder.append(compileLineMessage);
                }
                executeMessage.setNormalMessage(normalMessageBuilder.toString());
                normalReader.close();
                //获取错误流，打印错误信息
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String compileErrorMessage;
                StringBuilder errorMessageBuilder = new StringBuilder();
                while ((compileErrorMessage = errorReader.readLine()) != null) {
                    errorMessageBuilder.append(compileErrorMessage);
                }
                executeMessage.setErrorMessage(errorMessageBuilder.toString());
                errorReader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }
}
