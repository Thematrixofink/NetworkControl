package com.inkslab.networkManager.utils;

import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

/**
 * 网络工具类
 *

 */
public class NetUtils {

    /**
     * 获取客户端 IP 地址
     *
     * @param request
     * @return
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if (ip.equals("127.0.0.1")) {
                // 根据网卡取本机配置的 IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (inet != null) {
                    ip = inet.getHostAddress();
                }
            }
        }
        // 多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ip != null && ip.length() > 15) {
            if (ip.indexOf(",") > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }
        if (ip == null) {
            return "127.0.0.1";
        }
        return ip;
    }


    /**
     * 校验带宽单位是否合法
     * @param bandWidth
     * @return
     */
    public static boolean validBandWidth(String bandWidth){
        String BIT_RATE_PATTERN = "^\\d+([kKmMgG])bit$";
        Pattern pattern = Pattern.compile(BIT_RATE_PATTERN);
        Matcher matcher = pattern.matcher(bandWidth);
        return matcher.matches();
    }


    /**
     * 校验IP是否合法
     * @param ip
     * @return
     */
    public static boolean validIp(String ip){
        String IPV4_PATTERN =
                "^(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\." +
                        "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\." +
                        "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)\\." +
                        "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9][0-9]?)$";
        return Pattern.matches(IPV4_PATTERN, ip);
    }

    /**
     * 校验延迟是否合法
     * @param delay
     * @return
     */
    public static boolean validDelay(String delay){
        String DELAY_PATTERN = "^[0-9]+(s|ms)$";
        Pattern pattern = Pattern.compile(DELAY_PATTERN);
        Matcher matcher = pattern.matcher(delay);
        return matcher.matches();
    }

}
