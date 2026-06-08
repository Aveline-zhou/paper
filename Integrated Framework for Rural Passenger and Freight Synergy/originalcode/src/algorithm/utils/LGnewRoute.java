package algorithm.utils;

import algorithm.initial.LogisticRouteGenerator;
import model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 继承原有类，直接复用所有方法
public class LGnewRoute extends LogisticRouteGenerator {

    // ALNS后物流路径重生成（仅改过滤逻辑，其余调用父类）
    public void regenerateAfterALNS(Solution solution, Node distributionCenter) {
        int timeWindow = solution.getTimeWindow();
        solution.setDistributionCenter(distributionCenter);
        // 1. 读取物流节点
        Set<Node> logisticServiceNodes = solution.getLogisticServiceNodes();
        List<Node> validServiceNodes = new ArrayList<>();
        // 2. 核心：放宽过滤（去掉时段/IG校验，直接保留有效节点）
        for (Node node : logisticServiceNodes) {
            if (node instanceof ServiceStation) {
                validServiceNodes.add(node); // 服务站直接保留
            } else if (node instanceof Customer) {
                // 物流客户直接保留（不管时段，适配ALNS跨时段算子）
                validServiceNodes.add(node);
            }
        }
        if (validServiceNodes.isEmpty()) return;

        // 3. 直接调用父类的核心方法（一行逻辑都不复制）
        List<Route> initialRoutes = super.generateInitialRoutes(validServiceNodes, distributionCenter, timeWindow);
        List<VehicleSchedule> schedules = super.scheduleRoutesWithTime(initialRoutes, distributionCenter);
        List<Route> finalRoutes = super.extractRoutesFromSchedules(schedules);

        // 4. 更新路径
        solution.setLogisticRoutes(finalRoutes);
       // System.out.println("ALNS后物流路径：[" + finalRoutes.get(0).getNodes().stream().map(Node::getId).collect(Collectors.joining(", ")) + "]");
    }
}