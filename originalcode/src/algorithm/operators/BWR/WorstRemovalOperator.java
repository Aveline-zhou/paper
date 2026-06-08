package algorithm.operators.BWR;

import algorithm.operators.RemovalOperator;
import model.Solution;
import model.Customer;
import model.Route;
import java.util.*;
import java.util.Comparator;


public class WorstRemovalOperator implements RemovalOperator {
    private double weight = 1.0;       // 初始权重
    private int score = 0;             // 算子分数
    private int usageCount = 0;        // 使用次数
    private final double distributionFactor = 0.5; // 分布因子（可调整）

    @Override
    public String getName() {
        return "WorstRemoval";
    }

    @Override
    public Map<Integer, List<Customer>> remove(Map<Integer, Solution> allSolutions, int nq) {
        Map<Integer, List<Customer>> timeWindowToRemoved = new HashMap<>();
        // 遍历所有时段的解
        for (Map.Entry<Integer, Solution> entry : allSolutions.entrySet()) {
            int timeWindow = entry.getKey();
            Solution solution = entry.getValue();
            // 调用原有单时段的remove方法（下面的原有方法），获取该时段移除的客户
            List<Customer> removed = this.remove(solution, nq); // 直接调用原有单时段方法
            timeWindowToRemoved.put(timeWindow, removed);
        }
        incrementUsageCount();
        return timeWindowToRemoved;
    }


    @Override
    public List<Customer> remove(Solution solution, int nq) {
        // System.out.println("\n=== 最差移除算子开始 ===");


        // 1. 显式获取所有顾客（组合公交+物流服务的顾客），避免依赖getAllCustomers()
        List<Customer> allCustomers = new ArrayList<>();
        allCustomers.addAll(solution.getBusServiceCustomers());
        allCustomers.addAll(solution.getLogisticServiceCustomers());

        // 边界处理：无顾客可移除时直接返回
        if (allCustomers.isEmpty()) {
            // System.out.println("警告：解中无顾客可移除");
            // System.out.println("=== 最差移除算子结束 ===");
            return new ArrayList<>();
        }

        // 2. 输出基础信息，方便调试（已注释）
        // System.out.println("移除前 - 公交顾客数: " + solution.getBusServiceCustomers().size() +
        //         ", 物流顾客数: " + solution.getLogisticServiceCustomers().size() +
        //         ", 总顾客数: " + allCustomers.size());
        // System.out.println("请求移除顾客数: " + nq);
        int removeCount = Math.min(nq, allCustomers.size()); // 实际可移除的最大数量

        // 3. 计算每个顾客的移除收益（模拟方式：不修改原始解）
        List<RemovalGain> gainList = new ArrayList<>();
        double originalTotalCost = calculatePathCost(solution);
        // System.out.println("原始解总路径成本: " + originalTotalCost);

        for (Customer customer : allCustomers) {
            double gain = calculateRemovalGain(solution, customer, originalTotalCost);
            gainList.add(new RemovalGain(customer, gain));
            // System.out.println("顾客 " + customer.getId() + " - 移除收益: " + gain +
            //         " (服务模式: " + (customer.isServedByBus() ? "公交" : "物流") + ")");
        }

        // 4. 按收益降序排序（收益越高，移除后成本减少越多，越优先移除）
        gainList.sort(Comparator.comparingDouble(RemovalGain::getGain).reversed());
        // System.out.println("\n按收益降序排序后的前" + removeCount + "个顾客:");
        // for (int i = 0; i < removeCount && i < gainList.size(); i++) {
        //     RemovalGain gain = gainList.get(i);
        //     System.out.println("排名 " + (i+1) + ": 顾客 " + gain.getCustomer().getId() +
        //             ", 收益 " + gain.getGain());
        // }

        // 5. 执行最终的永久移除
        List<Customer> removedCustomers = new ArrayList<>();
        for (int i = 0; i < removeCount && i < gainList.size(); i++) {
            Customer customer = gainList.get(i).getCustomer();
            if (permanentlyRemoveCustomer(solution, customer)) {
                removedCustomers.add(customer);
                // System.out.println("成功移除顾客: " + customer.getId() +
                //         " (当前已移除 " + removedCustomers.size() + " 个)");
            } else {
                // System.out.println("警告：顾客 " + customer.getId() + " 移除失败（未找到对应路径或服务集合）");
            }
        }


        incrementUsageCount();
        return removedCustomers;
    }

    /**
     * 计算单个顾客的移除收益（模拟方式：不修改原始解）
     * @param originalSolution 原始解
     * @param customer 待移除顾客
     * @param originalCost 原始解总成本（避免重复计算）
     * @return 移除收益（成本减少量）
     */
    private double calculateRemovalGain(Solution originalSolution, Customer customer, double originalCost) {
        // 复制临时解，在临时解上模拟移除
        Solution tempSolution = originalSolution.copy();
        permanentlyRemoveCustomer(tempSolution, customer); // 临时解中执行永久移除
        double costAfterRemoval = calculatePathCost(tempSolution);
        return originalCost - costAfterRemoval; // 收益 = 移除前成本 - 移除后成本
    }

    /**
     * 计算解的总路径成本（公交路径 + 物流路径）
     */
    private double calculatePathCost(Solution solution) {
        double totalCost = 0.0;
        // 累加公交路径成本
        for (Route route : solution.getBusRoutes()) {
            totalCost += route.getTotalCost();
        }
        // 累加物流路径成本
        for (Route route : solution.getLogisticRoutes()) {
            totalCost += route.getTotalCost();
        }
        return totalCost;
    }

    private boolean permanentlyRemoveCustomer(Solution solution, Customer customer) {
        boolean removedFromRoutes = false;
        boolean removedFromServiceSets = false;

        // 1. 从公交路径移除
        for (Route route : solution.getBusRoutes()) {
            if (route.removeNode(customer)) {
                removedFromRoutes = true;
            }
        }
        // 2. 从物流路径移除
        for (Route route : solution.getLogisticRoutes()) {
            if (route.removeNode(customer)) {
                removedFromRoutes = true;
            }
        }
        // 3. 从服务集合移除
        boolean removedFromBus = solution.getBusServiceCustomers().remove(customer);
        boolean removedFromLogistic = solution.getLogisticServiceCustomers().remove(customer);
        removedFromServiceSets = removedFromBus || removedFromLogistic;
        // 4. 核心修复：更新顾客的服务状态（避免后续逻辑误判）
        if (removedFromBus) {
            customer.setServedByBus(false);
        }
        removedFromServiceSets = solution.removeCustomer(customer);

        return removedFromRoutes || removedFromServiceSets;
    }

    private static class RemovalGain {
        private final Customer customer;
        private final double gain;

        public RemovalGain(Customer customer, double gain) {
            this.customer = customer;
            this.gain = gain;
        }

        public Customer getCustomer() {
            return customer;
        }

        public double getGain() {
            return gain;
        }
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public void updateWeight(double newWeight) {
        this.weight = newWeight;
    }

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public void updateScore(int score) {
        this.score += score;
    }

    @Override
    public void resetScore() {
        this.score = 0;
    }

    @Override
    public int getUsageCount() {
        return usageCount;
    }

    @Override
    public void incrementUsageCount() {
        this.usageCount++;
    }

    @Override
    public void resetUsageCount() {
        this.usageCount = 0;
    }
}