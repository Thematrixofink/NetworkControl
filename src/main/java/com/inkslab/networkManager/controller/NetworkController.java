package com.inkslab.networkManager.controller;

import cn.hutool.core.util.StrUtil;
import com.inkslab.networkManager.common.BaseResponse;
import com.inkslab.networkManager.common.ErrorCode;
import com.inkslab.networkManager.common.ResultUtils;
import com.inkslab.networkManager.config.ClusterConfig;
import com.inkslab.networkManager.exception.BusinessException;
import com.inkslab.networkManager.exception.ThrowUtils;
import com.inkslab.networkManager.model.dto.bandwidthControl.DeleteControlRequest;
import com.inkslab.networkManager.model.dto.bandwidthControl.SetControlRequest;
import com.inkslab.networkManager.model.entity.*;
import com.inkslab.networkManager.service.NetworkService;
import com.inkslab.networkManager.utils.NetUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 带宽控制
 */
@RestController
@RequestMapping("/network")
@Slf4j
public class NetworkController {

    @Resource
    private ClusterConfig config;

    @Resource
    private NetworkService networkService;

    /**
     * 设置带宽
     * @param setControlRequest
     * @return
     */
    @PostMapping("/set/bandwidth")
    public BaseResponse<String> setBandWidth(@RequestBody SetControlRequest setControlRequest){
        Map<String, Server> clusterInfo = config.getClusterInfo();
        //1.检查参数
        String local = setControlRequest.getLocal();
        String target = setControlRequest.getTarget();
        String bandwidth = setControlRequest.getBandwidth();
        String delay = setControlRequest.getDelay();
        ThrowUtils.throwIf(!clusterInfo.containsKey(local),ErrorCode.NOT_FOUND_ERROR,"源服务器不存在,请配置集群信息!");
        ThrowUtils.throwIf(!clusterInfo.containsKey(target),ErrorCode.NOT_FOUND_ERROR,"目标服务器不存在,请配置集群信息!");
        //带宽参数校验
        ThrowUtils.throwIf(!NetUtils.validBandWidth(bandwidth),new BusinessException(ErrorCode.PARAMS_ERROR,"带宽大小或单位错误!"));
        ThrowUtils.throwIf(!NetUtils.validDelay(delay),new BusinessException(ErrorCode.PARAMS_ERROR,"延迟大小或单位错误!"));
        //2.检查是否已经存在控制
        boolean exit = networkService.getControlInfo(local).stream().anyMatch(control -> control.getTargetIp().equals(target));
        if(exit){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,local+"->"+target+"的控制已存在!");
        }
        //3.设置带宽
        String setResult = networkService.setBandwidth(setControlRequest);
        //4.返回结果
        //如果控制台输出不为空的话，说明控制带宽失败了
        if(!StrUtil.isBlank(setResult) || !StrUtil.isEmpty(setResult)){
            log.error(setResult);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"设置带宽失败!");
        }
        return  ResultUtils.success(setResult);
    }

    /**
     * 删除具体控制
     * 只删除route以及对应的子分类
     * @param deleteControlRequest
     * @return
     */
    @PostMapping("/delete/control")
    public BaseResponse<String> deleteControl(@RequestBody DeleteControlRequest deleteControlRequest){
        //1.参数校验
        Map<String, Server> clusterInfo = config.getClusterInfo();
        String localIp = deleteControlRequest.getLocalIp();
        String targetIp = deleteControlRequest.getTargetIp();
        ThrowUtils.throwIf(!clusterInfo.containsKey(localIp),ErrorCode.NOT_FOUND_ERROR,"源服务器不存在,请配置集群信息!");
        ThrowUtils.throwIf(!clusterInfo.containsKey(targetIp),ErrorCode.NOT_FOUND_ERROR,"目标服务器不存在,请配置集群信息!");
        //2.获取删除控制所需要的信息
        String output = networkService.deleteControl(localIp, targetIp);
        return ResultUtils.success(output);
    }

    /**
     * 删除根序列，也就是删除本机所有的控制
     * @param ip
     * @return
     */
    @PostMapping("/delete/root")
    public BaseResponse<String> deleteRoot(@RequestParam("ip")String ip){
        //1.校验ip是否在集群中
        validIpInfo(ip);
        //2.删除根序列
        String output = networkService.deleteRoot(ip);
        if(StrUtil.isNullOrUndefined(output) || !StrUtil.isBlank(output)){
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR,output);
        }
        return ResultUtils.success("删除根序列成功!");
    }

    /**
     * 查看某台服务器上根序列(队列)的信息
     * @param ip 服务器Ip
     * @return
     */
    @GetMapping("/get/root")
    public BaseResponse<List<QDisc>> getQDiscInfo(@RequestParam("ip")String ip){
        //1.校验服务器信息
        validIpInfo(ip);
        //2.执行查询
        List<QDisc> qDiscInfo = networkService.getQDiscInfo(ip);
        if(qDiscInfo == null){
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR,"未查询到根序列!");
        }
        return ResultUtils.success(qDiscInfo);
    }


    /**
     * 获取某台机器上的所有分类的信息
     * @param ip 要查看的服务器的IP
     * @return
     * @throws IOException
     */
    @GetMapping("/get/class")
    public BaseResponse<List<Clazz>> getClassInfo(@RequestParam("ip")String ip){
        //1.校验服务器信息
        validIpInfo(ip);
        //2.获取分类信息
        List<Clazz> classInfo = networkService.getClassInfo(ip);
        return ResultUtils.success(classInfo);
    }

    /**
     * 查询某台机器上的路由规则
     * @param ip
     * @return
     */
    @GetMapping("/get/route")
    public BaseResponse<List<Route>> getRouteInfo(@RequestParam("ip")String ip){
        //1.校验服务器信息
        validIpInfo(ip);
        //2.获取路由表信息
        List<Route> routeInfo = networkService.getRouteInfo(ip);
        if(routeInfo == null || routeInfo.isEmpty()){
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR,"未查询到路由信息");
        }
        return ResultUtils.success(routeInfo);
    }

    /**
     * 获取某台机器上的过滤器信息
     * @param ip
     * @return
     */
    @GetMapping("/get/filter")
    public BaseResponse<List<Filter>> getFilterInfo(@RequestParam("ip")String ip){
        //1.校验服务器信息
        validIpInfo(ip);
        //2.获取Filter信息
        List<Filter> filterInfo = networkService.getFilterInfo(ip);
        if(filterInfo == null || filterInfo.isEmpty()){
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR,"未查询到过滤器信息");
        }
        return ResultUtils.success(filterInfo);
    }

    /**
     * 获取控制信息
     * @param ip
     * @return
     */
    @GetMapping("/get/control")
    public BaseResponse<List<Control>> getControlInfo(@RequestParam("ip")String ip){
        //1.校验服务器信息
        validIpInfo(ip);
        //2.获取控制信息
        List<Control> result = networkService.getControlInfo(ip);
        if(result == null || result.isEmpty()){
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR,"未查询到控制信息");
        }
        return ResultUtils.success(result);
    }

    /**
     * 获取全部的网络拓扑信息
     * @return
     */
    @GetMapping("/get/all")
    public BaseResponse<List<Control>> getAllInfo(){
        //1.获取所有的ip以及具体的信息
        List<Server> servers = config.getServers();
        //2.获取全部的控制信息
        List<Control> allControlInfo = networkService.getAllInfo(servers);
        return ResultUtils.success(allControlInfo);
    }



    /**
     * 检验IP是否在集群配置信息中
     * @param ip
     * @return
     */
    private boolean validIpInfo(String ip){
        //1.校验服务器信息
        Map<String, Server> clusterInfo = config.getClusterInfo();
        if(!clusterInfo.containsKey(ip)){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"此IP不在集群信息中!请配置该服务器信息!");
        }
        return true;
    }
}
