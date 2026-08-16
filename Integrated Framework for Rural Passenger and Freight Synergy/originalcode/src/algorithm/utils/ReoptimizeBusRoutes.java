//package algorithm.utils;
//
//import model.Customer;
//import model.Node;
//import model.Route;
//import model.Solution;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * ALNS算法后用：将不可行的公交车路径通过插入服务站转为可行路径
// * 严格基于现有结构：
// * 1. 单时段解：Solution类（对应单个时段，含公交路径、服务集合、成本）
// * 2. ALNS全局解：Map<Integer, Solution>（key=时段，value=单时段解，包含所有时段的路径、服务集合、总成本）
// * 核心依赖通用的IG工具类处理路径逻辑
// */
//public class ReoptimizeBusRoutes {
//    // 依赖通用的IG工具类（核心路径处理逻辑封装在IG中，与你原有逻辑一致）
//    private final IG ig;
//
//    /**
//     * 构造方法：传入候选服务站列表（与初始解使用的列表完全一致）
//     * @param candidateServiceStations 候选服务站列表（你的原有参数）
//     */
//    public ReoptimizeBusRoutes(List<Node> candidateServiceStations) {
//        this.ig = new IG(candidateServiceStations);
//    }
//    /**
//     * 处理ALNS后的全局解（唯一核心方法，贴合你的Map<Integer, Solution>结构）
//     * @param alnsGlobalSolution ALNS后的全局解（包含所有时段的Solution，路径可能不可行）
//     * @param allCustomers 所有客户列表（用于计算节点需求，你的原有参数）
//     */
//    public void reoptimizeBusRoutesForALNS(Map<Integer, Solution> alnsGlobalSolution, List<Customer> allCustomers) {
//        // 遍历ALNS全局解中的每一个时段解（贴合你的多时段结构）
//        for (Map.Entry<Integer, Solution> entry : alnsGlobalSolution.entrySet()) {
//            int timeWindow = entry.getKey(); // 时段
//            Solution singlePeriodSolution = entry.getValue(); // 该时段的单时段解
//
//            // 步骤1：清空该时段解中ALNS修改后失效的旧服务站（你的核心逻辑，必须做）
//            singlePeriodSolution.clearServiceStations();
//
//            // 步骤2：调用IG类处理该时段的公交路径（ALNS后专属参数：过滤客户+过滤节点）
//            // 注：IG类的processBusRoutes方法已适配：
//            // - isFilterBusCustomers=true：仅处理该时段解的busServiceCustomers（ALNS修改后的集合）
//            // - isFilterNodes=true：过滤掉非公交服务的客户节点（ALNS移除的客户）
//            List<Route> feasibleBusRoutes = ig.processBusRoutes(singlePeriodSolution, allCustomers, true, true);
//
//            // 步骤3：更新该时段解的公交路径为可行路径（覆盖原有不可行路径）
//            singlePeriodSolution.setBusRoutes(feasibleBusRoutes);
//
//    }
//
//}}
package algorithm.utils;

import model.Customer;
import model.Node;
import model.Route;
import model.Solution;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 最终适配版：完全基于你现有Route类编写，无任何Route类修改
 * 核心适配点：
 * 1. 利用Route已有的setNodes(List<Node>)方法（自动同步demands/totalDemand）
 * 2. 不手动操作demands/totalDemand，依赖Route内部自动计算
 * 3. 保留原始核心逻辑+按规则分配客户+保证路径完整
 */
public class ReoptimizeBusRoutes {
    private final IG ig;

    public ReoptimizeBusRoutes(List<Node> candidateServiceStations) {
        this.ig = new IG(candidateServiceStations);
    }

    public void reoptimizeBusRoutesForALNS(Map<Integer, Solution> alnsGlobalSolution, List<Customer> allCustomers) {
        for (Map.Entry<Integer, Solution> entry : alnsGlobalSolution.entrySet()) {
            Solution singlePeriodSolution = entry.getValue();

            // 原始逻辑：清空失效服务站
            singlePeriodSolution.clearServiceStations();

            // 核心：按规则分配客户（适配现有Route）
            List<Route> originalRoutes = singlePeriodSolution.getBusRoutes();
            if (!originalRoutes.isEmpty()) {
                assignCustomerToRoutes(originalRoutes, singlePeriodSolution);
            }

            // 原始逻辑：IG处理+更新路径
            List<Route> feasibleBusRoutes = ig.processBusRoutes(singlePeriodSolution, allCustomers, true, true);
            singlePeriodSolution.setBusRoutes(feasibleBusRoutes);
        }
    }

    /**
     * 核心：按IDrange规则分配客户，仅替换数字节点，复用Route.setNodes自动同步需求
     */
    private void assignCustomerToRoutes(List<Route> originalRoutes, Solution solution) {
        Set<String> assignedCustomerIds = new HashSet<>();
        Set<Customer> busServiceCustomers = solution.getBusServiceCustomers();

        for (Route route : originalRoutes) {
            List<Node> newNodes = new ArrayList<>();
            List<Node> oldNodes = route.getNodes();

            // 遍历原始节点，仅替换数字节点，保留所有节点（保证路径完整）
            for (Node node : oldNodes) {
                String nodeId = node.getId();
                Integer numNode = parseNumberNodeByIDrange(nodeId);

                if (numNode != null) {
                    // 匹配「同开头数字+未分配」的客户
                    List<Customer> matchCustomers = busServiceCustomers.stream()
                            .filter(c -> !assignedCustomerIds.contains(c.getId()))
                            .filter(c -> isCustomerMatchNumber(c, numNode))
                            .sorted(Comparator.comparing(Customer::getId))
                            .collect(Collectors.toList());

                    if (!matchCustomers.isEmpty()) {
                        // 替换数字节点为客户（保留顺序）
                        for (Customer c : matchCustomers) {
                            newNodes.add(c);
                            assignedCustomerIds.add(c.getId());
                        }
                    } else {
                        // 无匹配客户，保留原数字节点
                        newNodes.add(node);
                    }
                } else {
                    // 非数字节点（公交站），直接保留
                    newNodes.add(node);
                }
            }

            // 关键：调用Route已有setNodes方法（自动同步demands/totalDemand）
            // 无需手动setDemands/setTotalDemand，Route内部会自动计算
            route.setNodes(newNodes);
        }
    }

    /**
     * 调用IDrange解析数字节点（无硬编码）
     */
    private Integer parseNumberNodeByIDrange(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) return null;
        try {
            int num = Integer.parseInt(nodeId.trim());
            return (num >= IDrange.FIRST_CUSTOMER_ID && num <= IDrange.LAST_CUSTOMER_ID) ? num : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 判断客户ID开头数字是否匹配
     */
    private boolean isCustomerMatchNumber(Customer customer, int num) {
        String custId = customer.getId();
        if (custId == null || custId.length() < 1) return false;
        try {
            int prefixNum = Integer.parseInt(custId.substring(0, 1));
            return prefixNum == num;
        } catch (Exception e) {
            return false;
        }
    }
}