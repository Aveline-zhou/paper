package algorithm.operators;

import model.Solution;
import model.Customer;
import model.Route;
import model.Node;
import algorithm.utils.DistanceCalculator;
import java.util.*;

public class DistanceBasedRemovalOperator implements RemovalOperator {
    private Random random = new Random();
    private double weight = 1.0;
    private int score = 0;
    private int usageCount = 0;

    // 控制移除偏向性的参数
  //  private double probability = 0.5; // 偏向移除距离较远的客户

    @Override
    public String getName() {
        return "DistanceBasedRemoval";
    }

    @Override
    public Map<Integer, List<Customer>> remove(Map<Integer, Solution> allSolutions, int nq) {
        Map<Integer, List<Customer>> timeWindowToRemoved = new HashMap<>();

        for (Map.Entry<Integer, Solution> entry : allSolutions.entrySet()) {
            int timeWindow = entry.getKey();
            Solution solution = entry.getValue();

            List<Customer> removed = distanceBasedRemove(solution, nq);
            timeWindowToRemoved.put(timeWindow, removed);
        }

        usageCount++;
        return timeWindowToRemoved;
    }

    @Override
    public List<Customer> remove(Solution solution, int nq) {
        Map<Integer, Solution> tempMap = Collections.singletonMap(solution.getTimeWindow(), solution);
        Map<Integer, List<Customer>> tempRemoved = remove(tempMap, nq);
        return tempRemoved.getOrDefault(solution.getTimeWindow(), new ArrayList<>());
    }

    /**
     * 基于距离的移除策略
     */
    private List<Customer> distanceBasedRemove(Solution solution, int nq) {
        List<Customer> allCustomers = new ArrayList<>();
        allCustomers.addAll(solution.getBusServiceCustomers());
        allCustomers.addAll(solution.getLogisticServiceCustomers());

        if (allCustomers.size() <= nq) {
            // 如果客户数不足，移除所有客户
            List<Customer> removed = new ArrayList<>(allCustomers);
            for (Customer customer : removed) {
                removeCustomerFromSolution(solution, customer);
            }
            return removed;
        }

        // 计算每个客户到配送中心的距离
        Node distributionCenter = solution.getDistributionCenter();
        Map<Customer, Double> distanceMap = new HashMap<>();

        for (Customer customer : allCustomers) {
            double distance = DistanceCalculator.calculateEuclideanDistance(
                    distributionCenter, customer);
            distanceMap.put(customer, distance);
        }

        // 按距离排序
        List<Customer> sortedCustomers = new ArrayList<>(allCustomers);
        sortedCustomers.sort((c1, c2) -> Double.compare(
                distanceMap.get(c2), distanceMap.get(c1))); // 降序，距离远的在前

        // 使用轮盘赌选择要移除的客户（距离越远概率越高）
        List<Customer> removedCustomers = new ArrayList<>();
        for (int i = 0; i < nq; i++) {
            Customer selected = selectCustomerByRoulette(sortedCustomers, distanceMap);
            removedCustomers.add(selected);
            sortedCustomers.remove(selected);
            removeCustomerFromSolution(solution, selected);
        }

        return removedCustomers;
    }

    /**
     * 轮盘赌选择客户（距离越远被选中的概率越高）
     */
    private Customer selectCustomerByRoulette(List<Customer> customers,
                                              Map<Customer, Double> distanceMap) {
        double totalDistance = 0;
        for (Customer customer : customers) {
            totalDistance += distanceMap.get(customer);
        }

        // 构建累积概率
        double[] cumulativeProb = new double[customers.size()];
        double sum = 0;
        for (int i = 0; i < customers.size(); i++) {
            Customer customer = customers.get(i);
            sum += distanceMap.get(customer) / totalDistance;
            cumulativeProb[i] = sum;
        }

        // 生成随机数并选择
        double rand = random.nextDouble();
        for (int i = 0; i < cumulativeProb.length; i++) {
            if (rand <= cumulativeProb[i]) {
                return customers.get(i);
            }
        }

        return customers.get(customers.size() - 1); // 兜底
    }

    /**
     * 从解决方案中移除客户（复用原有逻辑）
     */
    private void removeCustomerFromSolution(Solution solution, Customer customer) {
        for (Route route : solution.getBusRoutes()) {
            route.removeNode(customer);
        }
        for (Route route : solution.getLogisticRoutes()) {
            route.removeNode(customer);
        }
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