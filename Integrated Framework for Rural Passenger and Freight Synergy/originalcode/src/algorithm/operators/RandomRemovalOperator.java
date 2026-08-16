package algorithm.operators;

import model.Solution;
import model.Customer;
import model.Route;

import java.util.*;

public class RandomRemovalOperator implements RemovalOperator {
    private Random random = new Random();
    private double weight = 1.0;
    private int score = 0;
    private int usageCount = 0;

    @Override
    public String getName() {
        return "RandomRemoval";
    }


    @Override
    public Map<Integer, List<Customer>> remove(Map<Integer, Solution> allSolutions, int nq) {
        Map<Integer, List<Customer>> timeWindowToRemoved = new HashMap<>();
        // 遍历所有时段的解
        for (Map.Entry<Integer, Solution> entry : allSolutions.entrySet()) {
            int timeWindow = entry.getKey(); // 当前时段
            Solution solution = entry.getValue(); // 当前时段的单时段解
            List<Customer> removed = singleTimeWindowRemove(solution, nq);
            // 将该时段的移除客户存入映射表
            timeWindowToRemoved.put(timeWindow, removed);
        }

        usageCount++;
        return timeWindowToRemoved;
    }

    @Override
    public List<Customer> remove(Solution solution, int nq) {
        // 把单时段解包装成Map，调用多时段方法
        Map<Integer, Solution> tempMap = Collections.singletonMap(solution.getTimeWindow(), solution);
        Map<Integer, List<Customer>> tempRemoved = remove(tempMap, nq);
        return tempRemoved.getOrDefault(solution.getTimeWindow(), new ArrayList<>());
    }

    private List<Customer> singleTimeWindowRemove(Solution solution, int nq) {
        // 1. 获取当前时段的所有客户（公交+物流）（和原逻辑一致）
        Set<Customer> allCustomers = new HashSet<>();
        allCustomers.addAll(solution.getBusServiceCustomers());
        allCustomers.addAll(solution.getLogisticServiceCustomers());
        // 2. 随机选择nq个客户（若客户数不足，选全部）（和原逻辑一致）
        List<Customer> customerList = new ArrayList<>(allCustomers);
        int removeCount = Math.min(nq, customerList.size());
        List<Customer> removedCustomers = new ArrayList<>();
        for (int i = 0; i < removeCount; i++) {
            int randomIndex = random.nextInt(customerList.size());
            Customer removed = customerList.remove(randomIndex);
            removedCustomers.add(removed);
            // 3. 从路径和服务集合中移除该客户
            removeCustomerFromSolution(solution, removed);
        }
        return removedCustomers;
    }

private void removeCustomerFromSolution(Solution solution, Customer customer) {
    // 从公交路径移除（和原逻辑一致）
    for (Route route : solution.getBusRoutes()) {
        route.removeNode(customer);
    }
    // 从物流路径移除（和原逻辑一致）
    for (Route route : solution.getLogisticRoutes()) {
        route.removeNode(customer);
    }
    // 从服务集合移除（和原逻辑一致）
    solution.getBusServiceCustomers().remove(customer);
    solution.getLogisticServiceCustomers().remove(customer);
}

    // ====================== BaseOperator接口实现 ======================
    @Override public double getWeight() { return weight; }
    @Override public void updateWeight(double newWeight) { this.weight = newWeight; }
    @Override public int getScore() { return score; }
    @Override public void updateScore(int score) { this.score += score; }
    @Override public void resetScore() { this.score = 0; }
    @Override public int getUsageCount() { return usageCount; }
    @Override public void incrementUsageCount() { this.usageCount++; }
    @Override public void resetUsageCount() { this.usageCount = 0; }
}