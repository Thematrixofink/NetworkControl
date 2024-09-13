package com.inkslab.networkManager.service.impl;

import com.inkslab.networkManager.common.ErrorCode;
import com.inkslab.networkManager.exception.BusinessException;
import com.jcraft.jsch.*;
import com.inkslab.networkManager.config.ClusterConfig;
import com.inkslab.networkManager.constant.TCCommandConstant;
import com.inkslab.networkManager.service.CommandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.util.List;


@Slf4j
@Service
public class CommandServiceImpl implements CommandService {

    @Resource
    private ClusterConfig config;

    @Override
    public String execShell(String ip, String command) {
        log.info("exec command:"+command);
        String result = "";
        Channel exec = null;
        Session session = null;
        int exitStatus = -1;
        String password = config.getPassword();
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(config.getUsername(),ip,22);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(3000);
            session.connect();

            exec = session.openChannel("exec");

            // 这里修改命令，使用sudo -S
            String sudoCommand = "echo " + password + " | sudo -S " + command;

            ((ChannelExec)exec).setCommand(sudoCommand);
            // 错误信息输出流，用于输出错误的信息，当exitstatus<0的时候
            ((ChannelExec)exec).setErrStream(System.err);
            exec.connect();

            InputStream in = exec.getInputStream();
            result = getOut(in);
            exitStatus = exec.getExitStatus();

        } catch (JSchException | IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"执行shell命令失败!");
        }finally {
            // 关闭sftpChannel
            if (exec != null && exec.isConnected()) {
                exec.disconnect();
            }
            // 关闭jschSesson流
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
        log.info("exec result:\n"+result);
        return result;
    }

    @Override
    public String execShells(String ip, List<String> commands) {
        String result = "";
        JSch jsch = new JSch();
        Channel exec = null;
        Session session = null;
        ChannelShell channelShell = null;
        int exitStatus = -1;
        String password = config.getPassword();
        try {
            // 创建会话
            session = jsch.getSession(config.getUsername(),ip, 22);
            session.setPassword(password);
            // 忽略主机密钥检查
            session.setConfig("StrictHostKeyChecking", "no");
            // 连接到远程服务器
            session.connect();
            // 将命令写入 Shell 通道
            for (String command : commands) {
                log.info("exec command:"+command);
                exec = session.openChannel("exec");
                // 这里修改命令，使用sudo -S
                String sudoCommand = "echo " + password + " | sudo -S " + command;
                ((ChannelExec)exec).setCommand(sudoCommand);
                // 错误信息输出流，用于输出错误的信息，当exitstatus<0的时候
                ((ChannelExec)exec).setErrStream(System.err);
                exec.connect();
                InputStream in = exec.getInputStream();
                result = getOut(in);
                exitStatus = exec.getExitStatus();
                System.out.println(result);
            }

        } catch (JSchException | IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"执行shell命令失败!");
        } finally {
            if (channelShell != null) {
                channelShell.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
        log.info("exec result:\n"+result);
        return result;
    }


    @Override
    public String getCreateRootQdiscCommand(String nicName) {
        return TCCommandConstant.CREATE_ROOT_QDISC_PRE + nicName + TCCommandConstant.CREATE_ROOT_QDISC_SUF;
    }

    @Override
    public String getCreateMainClassCommand(String nicName) {
        //sudo tc class add dev <server_adapter:enp96s0f1> parent 1:0 classid 1:1 htb rate 1000mbit
        return TCCommandConstant.CREATE_MAIN_CLASS_PRE
                + nicName + " parent 1:0 classid 1:1 htb rate "
                + config.getDefaultBandwidth()
                +" ceil " + config.getDefaultBandwidth();
    }

    @Override
    public String getCreateSonClassCommand(String nicName,String parentClassNum,String sonClassNum,String bandWidth) {
        //sudo tc class add dev <server_adapter:enp96s0f1> parent 1:1 classid 1:10 htb rate 100mbit
        return "sudo tc class add dev " + nicName + " parent 1:"+parentClassNum+" classid 1:"+sonClassNum+" htb rate "+bandWidth+" ceil "+bandWidth;
    }

    @Override
    public String getCreateFilterCommand(String nicName,String mainClassNum) {
        return TCCommandConstant.CREATE_FILTER_PRE
                + nicName
                + " parent 1:0 protocol ip prio 100 route to "+mainClassNum+" flowid 1:"+mainClassNum;
    }

    @Override
    public String getCreateRouteCommand(String target, String nicName, String local,String mainClassNum) {
        //ip route add 192.168.1.24 dev eth0 via 192.168.1.66 realm 2
        return TCCommandConstant.CREATE_ROUTE_PRE
                + target
                + TCCommandConstant.CREATE_ROUTE_MID1
                + nicName
                + TCCommandConstant.CREATE_ROUTE_MID2
                + local
                + " realm "+mainClassNum;
    }

    @Override
    public String getDeleteQDISCCommand(String localNicName) {
        return TCCommandConstant.DELETE_QDISC_PRE
                + localNicName
                + TCCommandConstant.DELETE_QDISC_SUF;
    }

    @Override
    public String getDeleteRouteCommand(String targetIp) {
        return TCCommandConstant.DELETE_ROUTE_PRE
                + targetIp;
    }

    @Override
    public String getDeleteClassCommand(String nicName,Integer realm) {
        ////sudo tc class del dev eth0 classid 1:10
        return "sudo tc class del dev " + nicName + " classid 1:" + realm;
    }

    @Override
    public String getDeleteFilterCommand(String localNicName, String pref, String handle) {
        //sudo tc filter del dev enp96s0f1 protocol ip pref 100 handle 0xffff0004 route
        return "sudo tc filter del dev " + localNicName + " protocol ip pref " + pref + " handle "+ handle + " route";
    }



    @Override
    public String getSetDelayCommend(String localNicName,String delay,String sonClassNum,String delayQdiscNum) {
        return "sudo tc qdisc add dev "+ localNicName +" parent 1:" + sonClassNum + " handle " + delayQdiscNum + ": netem delay "+ delay;
    }

    @Override
    public String getDelDelayCommand(String localIp, String targetIp) {
        return null;
    }


    /**
     * 获取输出到字符串中
     * @param inputStream
     * @return
     * @throws IOException
     */
    private String getOut(InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();
        BufferedReader brs = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        for (String line = brs.readLine(); line != null; line = brs.readLine()) {
            result.append(line);
            result.append("\n");
        }
        return result.toString();
    }
}
