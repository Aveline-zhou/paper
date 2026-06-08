package algorithm.operators;

import model.*;
import config.Parameters;
import algorithm.utils.DistanceCalculator;
import java.util.*;

import static algorithm.utils.IDrange.FIRST_CUSTOMER_ID;
import static algorithm.utils.IDrange.LAST_CUSTOMER_ID;

/**
 * 独立跨时段调整算子
 * 自主随机选择客户点尝试提前配送
 */
public class TimeWindowIndependentOperator implements IndependentOperator {
    private double weight = 1.0;
    private int score = 0;
    private int usageCount = 0;
    private Parameters parameters = Parameters.getInstance();

    // 调整参数
   // private static final int BUS_CAPACITY_EXTENSION = 0;

    // 硬编码：调整客户数量（可配置）
    private int adjustmentCount = 2;

    public TimeWindowIndependentOperator() {
        // System.out.println("独立跨时段调整算子初始化");
    }

    @Override
    public String getName() {
        return "TimeWindowIndependent";
    }

    // ========== 核心方法：独立运行 ==========
    @Override
    public void run(Map<Integer, Solution> allSolutions) {
        // System.out.println("\n=== 独立跨时段调整算子 ===");
        // System.out.println("计划调整 " + adjustmentCount + " 个客户点");

        // 收集所有可调整的客户
        List<AdjustmentTask> tasks = collectAdjustmentTasks(allSolutions);
        if (tasks.isEmpty()) {
            // System.out.println("没有找到可调整的客户");
            incrementUsageCount();
            return;
        }

        // 随机选择客户点
        Collections.shuffle(tasks);
        int actualAdjustments = Math.min(adjustmentCount, tasks.size());

        // 执行调整
        int successCount = 0;
        for (int i = 0; i < actualAdjustments; i++) {
            AdjustmentTask task = tasks.get(i);
            // System.out.println("\n处理客户: " + task.customer.getId() +
            //         " (当前时段: " + task.currentWindow + ")");

            if (tryAdjustCustomer(task, allSolutions)) {
                successCount++;
                // System.out.println("  ✅ 成功提前到时段 " + (task.currentWindow - 1));
            } else {
                // System.out.println("  ❌ 无法提前，放回原路径");
            }
        }

        // System.out.println("\n=== 统计 ===");
        // System.out.println("调整成功: " + successCount + "/" + actualAdjustments);
        incrementUsageCount();
    }

    /**
     * 调整任务类
     */
    private class AdjustmentTask {
        Customer customer;
        int currentWindow;
        Solution currentSolution;

        AdjustmentTask(Customer customer, int currentWindow, Solution currentSolution) {
            this.customer = customer;
            this.currentWindow = currentWindow;
            this.currentSolution = currentSolution;
        }
    }

    /**
     * 收集所有可调整的客户
     */
    private List<AdjustmentTask> collectAdjustmentTasks(Map<Integer, Solution> allSolutions) {
        List<AdjustmentTask> tasks = new ArrayList<>();

        for (Map.Entry<Integer, Solution> entry : allSolutions.entrySet()) {
            int window = entry.getKey();
            Solution solution = entry.getValue();
            // 只处理时段2和3（时段1无法提前）
            if (window <= 1) continue;

            // 收集所有客户（不区分公交/物流）
            for (Customer customer : solution.getAllCustomers()) {
                // 可选的筛选条件：只调整特定范围的客户
                int customerNumberId = extractCustomerNumberId(customer.getId());
                boolean isInRange = (customerNumberId >= FIRST_CUSTOMER_ID &&
                        customerNumberId <= LAST_CUSTOMER_ID);

                if (isInRange) {
                    tasks.add(new AdjustmentTask(customer, window, solution));
                }
            }
        }

        // System.out.println("找到 " + tasks.size() + " 个可调整的客户");
        return tasks;
    }

    /**
     * 提取客户数字ID
     */
    private int extractCustomerNumberId(String customerId) {
        try {
            String[] parts = customerId.split("T");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 尝试调整单个客户
     */
    private boolean tryAdjustCustomer(AdjustmentTask task, Map<Integer, Solution> allSolutions) {
        Customer customer = task.customer;
        int currentWindow = task.currentWindow;
        Solution currentSolution = task.currentSolution;

        // 目标时段
        int targetWindow = currentWindow - 1;
        Solution targetSolution = allSolutions.get(targetWindow);

        if (targetSolution == null) {
            // System.out.println("  目标时段 " + targetWindow + " 不存在");
            return false;
        }

        // 根据客户类型尝试调整
        if (customer.isServedByBus()) {
            return tryAdjustAsBusCustomer(customer, currentWindow, targetWindow,
                    currentSolution, targetSolution);
        } else {
            return tryAdjustAsLogisticCustomer(customer, currentWindow, targetWindow,
                    currentSolution, targetSolution);
        }
    }

    /**
     * 作为公交客户尝试提前
     */
    private boolean tryAdjustAsBusCustomer(Customer customer, int currentWindow,
                                           int targetWindow, Solution currentSolution,
                                           Solution targetSolution) {
        // System.out.println("  尝试作为公交客户提前");

        double customerDemand = customer.getNormalDemand();
        double extendedCapacity = parameters.getBusCapacity() ;

        // 在目标时段的公交路径中寻找插入位置
        for (Route route : targetSolution.getBusRoutes()) {
            if (route.getVehicle().getType() != Vehicle.VehicleType.BUS) continue;

            int insertIndex = findBestInsertIndexForBus(route);
            if (insertIndex == -1) continue;

            // 检查段需求
            double segmentDemand = calculateSegmentDemand(route, insertIndex);
            double newSegmentDemand = segmentDemand + customerDemand;

            // System.out.println("  段需求检查: " + segmentDemand + " + " + customerDemand +
            //         " = " + newSegmentDemand + " ≤ " + extendedCapacity);

            // 容量检查
            if (newSegmentDemand <= extendedCapacity) {
                // 尝试插入
                boolean inserted = route.insertNode(insertIndex, customer, customerDemand);
                if (inserted) {
                    completeAdjustment(customer, targetWindow, targetSolution, currentSolution, true);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 作为物流客户尝试提前
     */
    private boolean tryAdjustAsLogisticCustomer(Customer customer, int currentWindow,
                                                int targetWindow, Solution currentSolution,
                                                Solution targetSolution) {
        // System.out.println("  尝试作为物流客户提前");
        // ========== 新增步骤1：计算本时段运输的距离成本 ==========
// 1.1 找到客户在原时段物流路径中的位置
        double distanceToPrevNode = 0.0;
        Route originalLogisticRoute = null;
        int originalCustomerIndex = -1;
// 遍历原时段的所有物流路径，找到客户所在路径
        for (Route route : currentSolution.getLogisticRoutes()) {
            if (route.getVehicle().getType() == Vehicle.VehicleType.LOGISTIC
                    && route.getNodes().contains(customer)) {
                originalLogisticRoute = route;
                originalCustomerIndex = route.getNodes().indexOf(customer);
                break;
            }
        }
// 1.2 计算客户到原路径中上一个节点的距离（不排除配送中心）
        if (originalLogisticRoute != null && originalCustomerIndex > 0) {
            Node prevNode = originalLogisticRoute.getNodes().get(originalCustomerIndex - 1);
            distanceToPrevNode = DistanceCalculator.calculateEuclideanDistance(prevNode, customer);
        }
// 1.3 计算本时段距离成本 = 距离 × 单位距离成本
        double currentDistanceCost = distanceToPrevNode * parameters.getDistanceCost();


        double customerDemand = customer.getTotalDemand();
        double logisticCapacity = parameters.getLogisticCapacity();

        // 寻找最佳插入位置
        Route bestRoute = null;
        int bestIndex = -1;
        double minDistanceIncrement = Double.MAX_VALUE;

        for (Route route : targetSolution.getLogisticRoutes()) {
            if (route.getVehicle().getType() != Vehicle.VehicleType.LOGISTIC) continue;

            // 容量检查
            if (route.getTotalDemand() + customerDemand > logisticCapacity) continue;

            int insertIndex = findBestInsertIndexForLogistic(route);
            if (insertIndex == -1) continue;

            double distanceIncrement = calculateInsertionDistance(route, customer, insertIndex);

            if (distanceIncrement < minDistanceIncrement) {
                minDistanceIncrement = distanceIncrement;
                bestRoute = route;
                bestIndex = insertIndex;
            }
        }

        if (bestRoute != null) {
            boolean inserted = bestRoute.insertNode(bestIndex, customer, customerDemand);
            if (inserted) {
                completeAdjustment(customer, targetWindow, targetSolution, currentSolution, false);
                return true;
            }
        }

        return false;
    }

    /**
     * 计算段需求
     */
    private double calculateSegmentDemand(Route route, int insertIndex) {
        double segmentDemand = 0.0;
        List<Node> nodes = route.getNodes();

        // 向前查找前一个服务站
        for (int i = insertIndex - 1; i >= 0; i--) {
            Node node = nodes.get(i);
            if (node instanceof ServiceStation) break;
            if (node instanceof Customer) {
                segmentDemand += ((Customer) node).getNormalDemand();
            }
        }

        return segmentDemand;
    }

    /**
     * 完成调整操作
     */
    private void completeAdjustment(Customer customer, int targetWindow,
                                    Solution targetSolution, Solution currentSolution,
                                    boolean isBus) {
        // 添加到目标时段
        if (isBus) {
            targetSolution.addBusCustomer(customer);
            customer.setServedByBus(true);
        } else {
            targetSolution.addLogisticCustomer(customer);
            customer.setServedByBus(false);
        }

        // 更新配送时段
        customer.setActualDeliveryTime(targetWindow);

        // 从原时段移除
        removeFromOriginalWindow(customer, currentSolution);
    }

    /**
     * 从原时段移除客户
     */
    private void removeFromOriginalWindow(Customer customer, Solution solution) {
        solution.removeCustomer(customer);
        for (Route route : solution.getBusRoutes()) {
            route.removeNode(customer);
        }
        for (Route route : solution.getLogisticRoutes()) {
            route.removeNode(customer);
        }
    }

    /**
     * 为公交车客户查找插入位置
     */
    private int findBestInsertIndexForBus(Route route) {
        List<Node> nodes = route.getNodes();

        // 在第一个公交站点之后插入
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getType() == Node.NodeType.BUS_STOP && i + 1 < nodes.size()) {
                return i + 1;
            }
        }

        return 1; // 默认位置
    }

    /**
     * 为物流客户查找插入位置
     */
    private int findBestInsertIndexForLogistic(Route route) {
        List<Node> nodes = route.getNodes();

        // 在配送中心之后插入
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getType() == Node.NodeType.DISTRIBUTION_CENTER && i + 1 < nodes.size()) {
                return i + 1;
            }
        }

        return 1; // 默认位置
    }

    /**
     * 计算插入距离增量
     */
    private double calculateInsertionDistance(Route route, Customer customer, int insertIndex) {
        List<Node> nodes = route.getNodes();

        if (insertIndex <= 0 || insertIndex >= nodes.size()) {
            return Double.MAX_VALUE;
        }

        Node prevNode = nodes.get(insertIndex - 1);
        Node nextNode = nodes.get(insertIndex);

        return DistanceCalculator.calculateInsertionCost(prevNode, customer, nextNode);
    }

    // ========== 配置方法 ==========

    /**
     * 设置调整客户数量
     */
    public void setAdjustmentCount(int count) {
        this.adjustmentCount = Math.max(1, count);
    }

    // ========== BaseOperator接口方法 ==========

    @Override
    public double getWeight() { return weight; }

    @Override
    public void updateWeight(double newWeight) { this.weight = newWeight; }

    @Override
    public void updateScore(int score) { this.score += score; }

    @Override
    public void resetScore() { this.score = 0; this.usageCount = 0; }

    @Override
    public int getScore() { return score; }

    @Override
    public int getUsageCount() { return usageCount; }

    @Override
    public void incrementUsageCount() { this.usageCount++; }

    @Override
    public void resetUsageCount() { this.usageCount = 0; }
}