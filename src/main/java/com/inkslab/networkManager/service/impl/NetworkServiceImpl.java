package com.inkslab.networkManager.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.inkslab.networkManager.common.ErrorCode;
import com.inkslab.networkManager.config.ClusterConfig;
import com.inkslab.networkManager.constant.TCCommandConstant;
import com.inkslab.networkManager.exception.BusinessException;
import com.inkslab.networkManager.model.dto.bandwidthControl.SetControlRequest;
import com.inkslab.networkManager.model.entity.*;
import com.inkslab.networkManager.service.CommandService;
import com.inkslab.networkManager.service.NetworkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NetworkServiceImpl implements NetworkService {

    @Resource
    private ClusterConfig config;
    @Resource
    private CommandService commandService;
    @Override
    public List<Server> getClusterInfo() {
        return config.getServers();
    }

    @Override
    public String getDefaultBandwidth() {
        return config.getDefaultBandwidth();
    }


    @Override
    public String setBandwidth(SetControlRequest setControlRequest) {
        //获取一些配置信息、比如网卡的名称
        String local = setControlRequest.getLocal();
        String target = setControlRequest.getTarget();
        String bandwidth = setControlRequest.getBandwidth();
        String delay = setControlRequest.getDelay();
        List<String> commands = new ArrayList<>();
        Map<String, Server> clusterInfo = config.getClusterInfo();
        Server localServerInfo = clusterInfo.get(local);
        String localNicName = localServerInfo.getNicName();
        String localIp = localServerInfo.getIp();

        //1.获取创建根序列命令
        //sudo tc qdisc add dev <nicname> root handle 1:0 htb default 1
        String createRootCommand = commandService.getCreateRootQdiscCommand(localNicName);
        commands.add(createRootCommand);

        //2.获取创建主分类命令
        //sudo tc class add dev <server_adapter:enp96s0f1> parent 1:0 classid 1:1 htb rate 1000mbit
        String createMainClassCommand = commandService.getCreateMainClassCommand(localNicName);
        commands.add(createMainClassCommand);

        //获取最大新的子分类序号
        List<Clazz> classInfo = getClassInfo(localIp);
        String newClassNum = getMaxSonClassNum(classInfo);
        log.info("当前可用的最大的子集合编号为:"+newClassNum);

        //3.创建子分类命令
        //sudo tc class add dev <server_adapter:enp96s0f1> parent 1:1 classid 1:mainClassNum htb rate 100mbit
        String createSonClassCommand = commandService.getCreateSonClassCommand(localNicName,"1",newClassNum,bandwidth);
        commands.add(createSonClassCommand);

        //4.拼接创建过滤器命令
        //tc filter add dev eth0 parent 1:0 protocol ip prio 100 route to mainClassNum flowid 1:mainClassNum
        String createFilterCommand = commandService.getCreateFilterCommand(localNicName,newClassNum);
        commands.add(createFilterCommand);

        //5.拼接创建路由命令
        //todo 实际运行环境要改为本地的ip（内网）
        String createRouteCommand = commandService.getCreateRouteCommand(target,localNicName,localIp,newClassNum);
        commands.add(createRouteCommand);

        //6.拼接创建延迟命令
        String delayQdiscNum = newClassNum + "0";
        String createDelayCommand = commandService.getSetDelayCommend(localNicName,delay,newClassNum,delayQdiscNum);
        commands.add(createDelayCommand);

        //7.执行命令
        String result = commandService.execShells(localIp, commands);

        //todo 对输出结果进行处理，判断是否创建成功
        return result;
    }



    @Override
    public String deleteControl(String localIp, String targetIp) {
        //1.获取到网卡信息
        Map<String, Server> clusterInfo = config.getClusterInfo();
        Server localSeverInfo = clusterInfo.get(localIp);
        String localNicName = localSeverInfo.getNicName();
        //sudo tc class del dev eth0 classid 1:10
        //sudo ip route del <target_ip:192.168.1.171>
        String deleteRouteCommand = commandService.getDeleteRouteCommand(targetIp);

        //获取删除子分类的命令
        List<Route> collect = getRouteInfo(localIp).stream().filter(route -> route.getTargetIp().equals(targetIp)).collect(Collectors.toList());
        if (collect.size() == 0) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"未查询到对于该ip的控制!");
        if (collect.size() > 1)  throw new BusinessException(ErrorCode.SYSTEM_ERROR,"对该路由有多个控制!");
        Route route = collect.get(0);
        Integer realm = route.getRealm();
        //realm为子分类的子序列，也就是1:realm
        String deleteSonClassCommand = commandService.getDeleteClassCommand(localNicName,realm);

        //获取删除过滤器命令
        //sudo tc filter del dev enp96s0f1 protocol ip pref 100 handle 0xffff0004 route
        List<Filter> filterInfo = getFilterInfo(localIp);
        //从众多的Filter根据realm提取出和此分类绑定的的Filter
        List<Filter> targetFilterList = filterInfo.stream().filter(filter -> filter.getRealm() == realm).collect(Collectors.toList());
        if (targetFilterList.size() == 0) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"未查询到此分类下的Filter!");
        if (targetFilterList.size() > 1)  throw new BusinessException(ErrorCode.SYSTEM_ERROR,"对该分类有多个Filter!");
        Filter targetFilter = targetFilterList.get(0);
        String deleteFilterCommand = commandService.getDeleteFilterCommand(localNicName, targetFilter.getPref(), targetFilter.getHandle());

        List<String> commandList = new ArrayList<>();
        commandList.add(deleteRouteCommand);
        //一定要先删除过滤器，才能删除子分类，要不然会提示繁忙
        commandList.add(deleteFilterCommand);
        commandList.add(deleteSonClassCommand);
        String output = commandService.execShells(localIp, commandList);
        return output;
    }

    @Override
    public List<QDisc> getQDiscInfo(String ip) {
        Map<String, Server> clusterInfo = config.getClusterInfo();
        String nicName = clusterInfo.get(ip).getNicName();
        String command = TCCommandConstant.LS_QDISC + nicName;
        String result = commandService.execShell(ip, command);
        //qdisc htb 1: root refcnt 3 r2q 10 default 1 direct_packets_stat 68 direct_qlen 1000
        //Sent 14696 bytes 68 pkt (dropped 0, overlimits 0 requeues 0)
        //backlog 0b 0p requeues 0
        if (result.charAt(9) == '0') {
            return null;
        }
        List<QDisc> keyInfo = getQDiscKeyInfo(result);
        return keyInfo;
    }

    @Override
    public List<Clazz> getClassInfo(String ip) {
        Map<String, Server> clusterInfo = config.getClusterInfo();
        String nicName = clusterInfo.get(ip).getNicName();
        String command = TCCommandConstant.LS_CLASS + nicName;
        String result = commandService.execShell(ip, command);
        return getClazzKeyInfo(result);
    }

    @Override
    public List<Route> getRouteInfo(String ip) {
        //1.获取控制台输出
        String command = TCCommandConstant.SHOW_ROUTE_INFO;
        String output = commandService.execShell(ip, command);
        //2.提取关键信息
        List<Route> result = getRouteKeyInfo(ip, output);
        return result;
    }

    @Override
    public String deleteRoot(String ip) {
        Map<String, Server> clusterInfo = config.getClusterInfo();
        String nicName = clusterInfo.get(ip).getNicName();
        String deleteQDISCCommand = commandService.getDeleteQDISCCommand(nicName);
        String output = commandService.execShell(ip, deleteQDISCCommand);
        return output;
    }

    @Override
    public List<Control> getControlInfo(String ip) {
        //1.首先获取Route中的realm，以及目的IP
        //2.根据Realm找到Filter，获取Filter中的flowid
        //3.根据flowid找到对应的类，拿到带宽以及

        //1.获取必要信息
        List<Route> routes = getRouteInfo(ip);
        List<Filter> filters = getFilterInfo(ip);
        List<Clazz> classes = getClassInfo(ip);
        Map<String, QDisc> qDiscMap = getQDiscInfo(ip).stream().collect(Collectors.toMap(QDisc::getParent, qdisc -> qdisc));
        //2.按照realm值散列Filter
        Map<Integer, Filter> filterMap = new HashMap<>();
        for (Filter filter : filters) {
            filterMap.put(filter.getRealm(), filter);
        }
        //3.按照flowid散列类信息
        Map<String, Clazz> classMap = new HashMap<>();
        for (Clazz cls : classes) {
            classMap.put(cls.getName(), cls);
        }
        //4.对于每个Route进行操作，获取带宽值
        List<Control> results = new ArrayList<>();
        for (Route route : routes) {
            //根据realm获取到Filter
            Filter filter = filterMap.get(route.getRealm());
            if (filter != null) {
                //根据Filter中的flowid获取到类信息
                Clazz clazz = classMap.get(String.valueOf(filter.getFlowid()));
                if (clazz != null) {
                    String name = clazz.getName();
                    Control control = new Control();
                    if(qDiscMap.containsKey(name)){
                        control.setDelay(qDiscMap.get(name).getDelay());
                    }
                    control.setLocalIp(ip);
                    control.setTargetIp(route.getTargetIp());
                    control.setBandWidth(clazz.getRate());
                    results.add(control);
                }
            }
        }
        return results;
    }

    @Override
    public List<Control> getAllInfo(List<Server> servers) {
        //1.创建一个Map集合，便于快速查询不存在控制信息的服务器
        //Map<String, Set<String>> controlMap = new HashMap<>();
        Map<String, Map<String, Control>> controlMap = new HashMap<>();
        //2.获取所有的控制信息
        List<Control> allControlInfo = servers.stream()
                .map(server -> getControlInfo(server.getIp()))
                .flatMap(List::stream)
                .peek(control -> controlMap
                        .computeIfAbsent(control.getLocalIp(), k -> new HashMap<>())
                        .put(control.getTargetIp(), control))
                .collect(Collectors.toList());

        String defaultBandwidth = config.getDefaultBandwidth();

        //3.确保每一对服务器之间都有控制信息,对于不存在的添加默认带宽
        for (Server source : servers) {
            for (Server target : servers) {
                if (!source.equals(target) &&
                        !controlMap.getOrDefault(source.getIp(), Collections.emptyMap()).containsKey(target.getIp())) {
                    Control control = new Control();
                    control.setLocalIp(source.getIp());
                    control.setTargetIp(target.getIp());
                    control.setBandWidth(null);
                    control.setDefaultBandWidth(defaultBandwidth);
                    if (controlMap.containsKey(target.getIp())){
                        String delay = controlMap.get(target.getIp()).get(source.getIp()).getDelay();
                        control.setDelay(delay);
                    }else {
                        control.setDelay(null);
                    }
                    allControlInfo.add(control);
                }
            }
        }
        return allControlInfo;
    }


    @Override
    public List<Filter> getFilterInfo(String ip) {
        //tc filter show dev enp96s0f1
        Map<String, Server> clusterInfo = config.getClusterInfo();

        Server server = clusterInfo.get(ip);
        if(ObjUtil.isNull(server) || ObjUtil.isEmpty(server)){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"集群中未查询到该服务器信息!");
        }
        String nicName = server.getNicName();
        String showFilterCommand = TCCommandConstant.SHOW_FILTER_INFO + nicName;
        String output = commandService.execShell(ip, showFilterCommand);
        List<Filter> result = getFilterKeyInfo(ip, output);
        return result;
    }



    /**
     * 从输出中获取QDisc的关键信息
     *
     * @param rawOutput
     * @return
     */
    private List<QDisc> getQDiscKeyInfo(String rawOutput) {
        List<QDisc> result = new ArrayList<>();
        String regex = "qdisc\\s+(\\w+)\\s+(\\S+)(?:\\s+parent (\\S+))?(?:\\s+limit \\d+)?(?:\\s+delay (\\S+ms))?.*";
        Pattern pattern = Pattern.compile(regex);
        // 匹配输入字符串
        Matcher matcher = pattern.matcher(rawOutput);
        // 捕获队列规则名称
        while (matcher.find()) {
            String qdiscType = matcher.group(1);
            // 捕获标识符
            String qdiscId = matcher.group(2);
            String parent = matcher.group(3);
            String delay = matcher.group(4);

            QDisc temp = new QDisc();
            temp.setType(qdiscType);
            temp.setName(qdiscId);
            temp.setParent(parent != null ? parent : "root");
            temp.setDelay(delay != null ? delay : null);
            result.add(temp);
        }
        return result;
    }

    /**
     * 从输出中获取Class的关键信息
     *
     * @param rawOutput
     * @return
     */
    public List<Clazz> getClazzKeyInfo(String rawOutput) {
        // 正则表达式
        String regex = "class\\s+(\\w+)\\s+(\\S+)\\s+(root|parent\\s+(\\S+)).*?\\s+rate\\s+(\\S+)\\s+ceil\\s+(\\S+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(rawOutput);
        List<Clazz> result = new ArrayList<>();
        // 查找匹配
        while (matcher.find()) {
            // 捕获类型
            String classType = matcher.group(1);
            // 捕获名称
            String className = matcher.group(2);
            // 捕获root或parent
            String rootOrParent = matcher.group(3);
            // 捕获组5: rate值
            String rate = matcher.group(5);
            // 捕获组6: ceil值
            String ceil = matcher.group(6);
            Clazz clazz = new Clazz();
            clazz.setType(classType);
            clazz.setName(className);
            clazz.setRate(rate);
            clazz.setCeil(ceil);
            clazz.setFather(rootOrParent);
            result.add(clazz);
        }
        return result;
    }


    /**
     * 获取当前最大的可用的子分类序号
     * @param classInfo
     * @return
     */
    public String getMaxSonClassNum(List<Clazz> classInfo) {
        Map<Integer,Clazz> temp = new HashMap<>();
        for (Clazz clazz : classInfo) {
            if(!clazz.getFather().equals("root")){
                String className = clazz.getName();
                String[] split = className.split(":");
                String num = split[1];
                temp.put(Integer.parseInt(num),clazz);
            }
        }
        if(temp.size() == 0){
            return String.valueOf(2);
        }
        //todo i为子集合的上限
        for(int i = 2 ; i <= 100 ; i++){
            if(!temp.containsKey(i)){
                return String.valueOf(i);
            }
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR,"达到能创建的最大子集合数目!");
    }

    /**
     * 从众多路由表信息中，提取出来我们创立的路由信息
     * @param localIp
     * @param out
     * @return
     */
    private List<Route> getRouteKeyInfo(String localIp,String out){
        List<Route> result = new ArrayList<>();
        // 正则表达式匹配
        String regex = "(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+via\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+dev\\s+(\\S+)\\s+realm\\s+(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(out);
        while (matcher.find()) {
            Route route = new Route();
            route.setTargetIp(matcher.group(1));
            route.setLocalIp(localIp);
            route.setNicName(matcher.group(3));
            route.setRealm(Integer.parseInt(matcher.group(4)));
            result.add(route);
        }
        return result;
    }

    /**
     * 从输出中提取Filter关键信息
     * @param ip
     * @param output
     * @return
     */
    private List<Filter> getFilterKeyInfo(String ip, String output) {
        List<Filter> result = new ArrayList<>();
        // 正则表达式
        String regex = "pref\\s+(\\d+)\\s+route\\s+chain\\s+(\\d+)\\s+fh\\s+(0xffff\\d{4})\\s+flowid\\s+(1:\\d+)\\s+to\\s+(\\d+)";
        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(output);
        // 提取匹配的信息
        while (matcher.find()) {
            Filter filter = new Filter();
            filter.setPref(matcher.group(1));
            filter.setChain(matcher.group(2));
            filter.setHandle(matcher.group(3));
            filter.setFlowid(matcher.group(4));
            filter.setRealm(Integer.parseInt(matcher.group(5)));
            result.add(filter);
        }
        return result;
    }


    /**
     * 判断已有的控制信息中是否有已存在的控制信息
     * @param controls
     * @param source
     * @param target
     * @return
     */
    private boolean hasControlInfo(List<Control> controls, Server source, Server target) {
        return controls.stream()
                .anyMatch(control -> control.getLocalIp().equals(source.getIp())
                        && control.getTargetIp().equals(target.getIp()));
    }

}
