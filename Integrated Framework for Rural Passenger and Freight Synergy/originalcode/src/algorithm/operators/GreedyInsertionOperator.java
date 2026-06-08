package algorithm.operators;

import model.Solution;
import model.Customer;
import model.Route;
import model.Node;
import model.Vehicle;
import config.Parameters;
import algorithm.utils.DistanceCalculator;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class GreedyInsertionOperator implements InsertionOperator {
    private double weight = 1.0;
    private int score = 0;
    private int usageCount = 0;
    private Parameters parameters = Parameters.getInstance();

    //尝试插入现有物流车路径，或新建
    @Override
    public String getName() {
        return "GreedyInsertion";
    }

    // ============== 新增：多时段插入方法（核心！！！） ==============
    public void insert(Map<Integer, Solution> allSolutions, Map<Integer, List<Customer>> timeWindowToRemovedCustomers) {
        // 遍历所有时段的解，执行对应时段的插入
        for (Map.Entry<Integer, Solution> entry : allSolutions.entrySet()) {
            int timeWindow = entry.getKey(); // 当前时段（如1、2、3）
            Solution solution = entry.getValue(); // 当前时段的单时段解
            // 获取该时段需要插入的客户（若没有则跳过）
            List<Customer> removedCustomers = timeWindowToRemovedCustomers.getOrDefault(timeWindow, Collections.emptyList());

            // 调试：打印当前时段的修复信息（已注释）
            // System.out.println("===== 时段 " + timeWindow + " 修复算子开始 =====");
            // System.out.println("时段 " + timeWindow + " 需要修复 " + removedCustomers.size() + " 个客户点");

            // 执行原有单时段的插入逻辑（完全复用）
            processSingleTimeWindowInsert(solution, removedCustomers, timeWindow);
        }
        incrementUsageCount();
    }

    @Override
    public void insert(Solution solution, List<Customer> removedCustomers) {
        // 调用新增的多时段方法（包装成单时段的Map）
        Map<Integer, Solution> tempMap = Collections.singletonMap(solution.getTimeWindow(), solution);
        Map<Integer, List<Customer>> tempRemoved = Collections.singletonMap(solution.getTimeWindow(), removedCustomers);
        insert(tempMap, tempRemoved);
        // 原有incrementUsageCount()已在多时段方法中执行，这里无需重复执行
    }

    private void processSingleTimeWindowInsert(Solution solution, List<Customer> removedCustomers, int timeWindow) {
        int originallyBus = 0;
        for (Customer customer : removedCustomers) {
            if (customer.isServedByBus()) {
                originallyBus++;
            }
        }
        // System.out.println("时段 " + timeWindow + " 其中原公交车服务客户: " + originallyBus);

        // ========== 新增代码（第1处：2行） ==========
        int keepBusCount = 0; // 新增：已保留的公交车客户数
        final int KEEP_BUS_NUM = 1; // 新增：固定保留2个
        // ===========================================

        for (Customer customer : removedCustomers) {
            // ========== 新增代码（第2处：3行） ==========
            if (customer.isServedByBus() && keepBusCount < KEEP_BUS_NUM) {
                keepBusCount++;
                continue; // 跳过后续物流车插入，保留公交车服务
            }
            // ===========================================
            boolean wasBus = customer.isServedByBus(); // 调试：记录原服务模式
            // 仅尝试插入物流车路径（现有 + 新建）（复用原有逻辑）
            boolean insertedToLogistic = tryInsertIntoLogisticRoute(solution, customer);

            // 调试：打印转换结果（已注释）
            if (wasBus && insertedToLogistic) {
                // System.out.println("时段 " + timeWindow + " ✅ 成功将公交车客户 " + customer.getId() + " 转为物流车服务");
            } else if (wasBus && !insertedToLogistic) {
                // System.out.println("时段 " + timeWindow + " ❌ 失败：公交车客户 " + customer.getId() + " 无法转为物流车服务");
            }

            if (insertedToLogistic) {
                continue; // 插入成功，处理下一个顾客
            }

            // 兜底：若新建物流路径也失败，打印警告（已注释）
            // System.err.println("时段 " + timeWindow + "  - ⚠️ 警告: 顾客 " + customer.getId() + " 无法插入任何物流路径，也无法新建物流路径！");
        }
    }

    private boolean tryInsertIntoLogisticRoute(Solution solution, Customer customer) {
        double  minDistanceIncrement = Double.MAX_VALUE;
        Route bestRoute = null;
        int bestIndex = -1;
        double customerDemand = customer.getTotalDemand(); // 物流车使用总需求
        // 尝试插入现有物流车路径
        for (Route route : solution.getLogisticRoutes()) {
            // 容量检查
            if (route.getTotalDemand() + customerDemand > route.getVehicle().getCapacity()) {
                continue;
            }
            List<Node> nodes = route.getNodes();// 遍历路径中所有可能的插入位置，跳过起点配送中心
            for (int i = 1; i < nodes.size(); i++) {
                Node prevNode = nodes.get(i - 1); // 插入位置的前一个节点（如1）
                Node nextNode = nodes.get(i);     // 插入位置的后一个节点（如2）
                double distanceIncrement = DistanceCalculator.calculateInsertionCost(prevNode, customer, nextNode);
                if (distanceIncrement < minDistanceIncrement) { // 原：if (costIncrease < minCostIncrease)
                    minDistanceIncrement = distanceIncrement;   // 原：minCostIncrease = costIncrease;
                    bestRoute = route;
                    bestIndex = i;
                }
            }
        }

        // 若找到合适的现有路径，执行插入
        if (bestRoute != null && bestIndex != -1) {
            bestRoute.insertNode(bestIndex, customer, customerDemand);
            solution.addLogisticCustomer(customer);
            customer.setServedByBus(false);
            // System.out.println("顾客 " + customer.getId() + " 插入现有物流车路径 " + bestRoute.getId());
            return true;
        }

        // 3. 若现有路径都无法容纳，尝试新建物流车路径
        return createNewLogisticRoute(solution, customer);
    }

    //为顾客创建一条全新的物流车路径
    private boolean createNewLogisticRoute(Solution solution, Customer customer) {
        // 1. 创建新车辆（不需要原型，直接创建）
        Vehicle newVehicle = new Vehicle(
                Vehicle.VehicleType.LOGISTIC,
                parameters.getLogisticCapacity()
        );
        newVehicle.setId("LOG_VEHICLE_NEW_" + System.currentTimeMillis());

        // 2. 创建新路径（起点/终点为配送中心，中间为当前顾客）
        Route newRoute = new Route(newVehicle, solution.getTimeWindow());
        Node distributionCenter = solution.getDistributionCenter();

        // 确保配送中心不为null
        if (distributionCenter == null) {
            // System.err.println("    - 错误：配送中心为null，无法创建物流车路径");
            return false;
        }

        newRoute.addNode(distributionCenter, 0.0);
        newRoute.addNode(customer, customer.getTotalDemand());
        newRoute.addNode(distributionCenter, 0.0);

        // 3. 将新路径添加到解中
        solution.addLogisticRoute(newRoute);
        solution.addLogisticCustomer(customer);
        customer.setServedByBus(false);
        // System.out.println("✅ 顾客 " + customer.getId() + " 新建物流车路径 " + newRoute.getId());
        return true;
    }

    // ====================== BaseOperator接口实现（无修改）======================
    @Override public double getWeight() { return weight; }
    @Override public void updateWeight(double newWeight) { this.weight = newWeight; }
    @Override public int getScore() { return score; }
    @Override public void updateScore(int score) { this.score += score; }
    @Override public void resetScore() { this.score = 0; }
    @Override public int getUsageCount() { return usageCount; }
    @Override public void incrementUsageCount() { this.usageCount++; }
    @Override public void resetUsageCount() { this.usageCount = 0; }
}