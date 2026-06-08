package algorithm.operators;

import model.Solution;
import model.Customer;
import model.Route;
import model.Node;
import model.Vehicle;
import config.Parameters;
import algorithm.utils.DistanceCalculator;
import java.util.*;

public class RegretInsertionOperator implements InsertionOperator {
    private double weight = 1.0;
    private int score = 0;
    private int usageCount = 0;//一致的
    private Parameters parameters = Parameters.getInstance();
    private int regretK = 2;

    public RegretInsertionOperator() {
        this.regretK = 2; }
//
//    public RegretInsertionOperator(int regretK) {
//        this.regretK = regretK;
//    }

    @Override
    public String getName() {
        return "Regret" + regretK + "Insertion";
    }

    @Override
    public void insert(Map<Integer, Solution> allSolutions,
                       Map<Integer, List<Customer>> timeWindowToRemovedCustomers) {// 遍历所有时段
        for (Map.Entry<Integer, Solution> entry : allSolutions.entrySet()) {
            int timeWindow = entry.getKey();
            Solution solution = entry.getValue();
            List<Customer> removedCustomers = timeWindowToRemovedCustomers.getOrDefault(
                    timeWindow, Collections.emptyList());
            processSingleTimeWindowRegretInsert(solution, removedCustomers, timeWindow);// 对当前时段执行后悔值插入
        }
        incrementUsageCount();}

    @Override
    public void insert(Solution solution, List<Customer> removedCustomers) { // 将单时段解包装成Map
        Map<Integer, Solution> tempMap = Collections.singletonMap(solution.getTimeWindow(), solution);
        Map<Integer, List<Customer>> tempRemoved = Collections.singletonMap(
                solution.getTimeWindow(), removedCustomers);
        insert(tempMap, tempRemoved);
    }

    /**
     * 单时段后悔值插入逻辑
     */
    private void processSingleTimeWindowRegretInsert(Solution solution,
                                                     List<Customer> customersToInsert,
                                                     int timeWindow) {
        if (customersToInsert.isEmpty()) {return;}
        // 先尝试所有客户新建路径
        List<Customer> waitingCustomers = new ArrayList<>(customersToInsert);
        List<Customer> toRemove = new ArrayList<>(); // 记录成功创建新路径的客户
        for (Customer customer : waitingCustomers) { // 尝试为当前客户创建新的物流路径
            if (createNewLogisticRoute(solution, customer)) { // 如果创建成功，标记为待移除
                toRemove.add(customer);}}
        waitingCustomers.removeAll(toRemove); // 从等待列表中移除已成功分配的客户

        // 剩余客户使用后悔值插入
        while (!waitingCustomers.isEmpty()) {
            double maxRegretValue = Double.NEGATIVE_INFINITY;
            Customer bestCustomer = null;
            Route bestRoute = null;
            int bestPosition = -1;
            for (Customer customer : waitingCustomers) {// 遍历所有剩余客户
                List<InsertionOption> topKOptions = findTopKInsertionOptions(
                        solution, customer, regretK);
                if (topKOptions.isEmpty()) {continue;} // 如果没有可行插入选项，跳过该客户然后计算当前客户的后悔值
                double regretValue = calculateRegretValue(topKOptions);
                if (regretValue > maxRegretValue) {//后悔值更大更新
                    maxRegretValue = regretValue;
                    bestCustomer = customer;
                    bestRoute = topKOptions.get(0).route;
                    bestPosition = topKOptions.get(0).position;
                }
            }// 最佳不为空就插入
            if (bestCustomer != null) {
                executeInsertion(solution, bestCustomer, bestRoute, bestPosition);
                waitingCustomers.remove(bestCustomer);
            } else {
                // 剩余客户全部新建路径
                for (Customer customer : waitingCustomers) {
                    createNewLogisticRoute(solution, customer);}
                break;}
        }
    }
    //查找前K个最佳插入选项
    private List<InsertionOption> findTopKInsertionOptions(Solution solution,
                                                           Customer customer,
                                                           int k) {
        List<InsertionOption> allOptions = new ArrayList<>(); // 存储所有可行插入选项
        double customerDemand = customer.getTotalDemand();//依旧获取需求

        // 1. 检查所有现有物流路径
        for (Route route : solution.getLogisticRoutes()) {
            if (route.getTotalDemand() + customerDemand > route.getVehicle().getCapacity()) {
                continue; // 容量不足
            }

            List<Node> nodes = route.getNodes();
            for (int i = 1; i < nodes.size(); i++) {
                Node prevNode = nodes.get(i - 1);
                Node nextNode = nodes.get(i);

                double distanceIncrease = DistanceCalculator.calculateInsertionCost(
                        prevNode, customer, nextNode);

                allOptions.add(new InsertionOption(route, i, distanceIncrease));
            }
        }

        // 2. 按成本升序排序
        allOptions.sort(Comparator.comparingDouble(o -> o.cost));

        // 3. 返回前K个选项
        return allOptions.subList(0, Math.min(k, allOptions.size()));
    }

    /**
     * 计算后悔值（支持不同阶数）
     */
    private double calculateRegretValue(List<InsertionOption> topKOptions) {
        if (topKOptions.isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }

        if (topKOptions.size() == 1) {
            // 只有一个选项，使用一个较大的默认值
            return 1000.0;
        }

        switch (regretK) {
            case 1:
                // regret-1: 最优成本的负值（越小越好）
                return -topKOptions.get(0).cost;

            case 2:
                // regret-2: 次优成本 - 最优成本
                return topKOptions.get(1).cost - topKOptions.get(0).cost;

            case 3:
                // regret-3: (次优+第三优) - 2*最优
                return (topKOptions.get(1).cost + topKOptions.get(2).cost)
                        - 2 * topKOptions.get(0).cost;

            default:
                // 通用regret-k: Σ(第i优) - (k-1)*最优
                double sum = 0;
                for (int i = 1; i < topKOptions.size(); i++) {
                    sum += topKOptions.get(i).cost;
                }
                return sum - (regretK - 1) * topKOptions.get(0).cost;
        }
    }

    /**
     * 执行插入操作
     */
    private void executeInsertion(Solution solution, Customer customer,
                                  Route route, int position) {
        double customerDemand = customer.getTotalDemand();

        route.insertNode(position, customer, customerDemand);
        solution.addLogisticCustomer(customer);
        customer.setServedByBus(false);

        // 记录日志
        // System.out.println("Regret插入: 客户 " + customer.getId() +
        //                   " 插入路径 " + route.getId() + " 位置 " + position);
    }

//    /**
//     * 处理剩余无法插入的客户
//     */
//    private void handleRemainingCustomers(Solution solution, List<Customer> remainingCustomers) {
//        for (Customer customer : remainingCustomers) {
//            if (!createNewLogisticRoute(solution, customer)) {
//                // 如果新建路径也失败，输出警告
//                System.err.println("警告: 客户 " + customer.getId() +
//                        " 无法插入任何路径");
//            }
//        }
//    }

    /**
     * 创建新物流路径（复用原有方法）
     */
    private boolean createNewLogisticRoute(Solution solution, Customer customer) {
        Vehicle newVehicle = new Vehicle(
                Vehicle.VehicleType.LOGISTIC,
                parameters.getLogisticCapacity()
        );
        newVehicle.setId("LOG_VEHICLE_" + System.currentTimeMillis() + "_" + UUID.randomUUID());

        Route newRoute = new Route(newVehicle, solution.getTimeWindow());
        Node distributionCenter = solution.getDistributionCenter();

        if (distributionCenter == null) {
            return false;
        }

        newRoute.addNode(distributionCenter, 0.0);
        newRoute.addNode(customer, customer.getTotalDemand());
        newRoute.addNode(distributionCenter, 0.0);

        solution.addLogisticRoute(newRoute);
        solution.addLogisticCustomer(customer);
        customer.setServedByBus(false);

        return true;
    }

    // ====================== 内部辅助类 ======================
    private static class InsertionOption {
        Route route;
        int position;
        double cost;

        InsertionOption(Route route, int position, double cost) {
            this.route = route;
            this.position = position;
            this.cost = cost;
        }
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