package algorithm.initial;
import config.Parameters;
import model.*;
import java.util.*;

public class InitialSolutionBuilder {
    private CustomerClassifier customerClassifier;
    private BusRouteGenerator busRouteGenerator;
    private ServiceStationInserter stationInserter;
    private LogisticRouteGenerator logisticRouteGenerator;
    private Parameters parameters;
    private List<Customer> allCustomers;

    public InitialSolutionBuilder(List<Node> candidateBusStops) {
        this.parameters = Parameters.getInstance();
        this.customerClassifier = new CustomerClassifier();
        this.busRouteGenerator = new BusRouteGenerator();
        this.stationInserter = new ServiceStationInserter(candidateBusStops); // 传入候选公交站点
        this.logisticRouteGenerator = new LogisticRouteGenerator();
    }
//为每个时段生成符合容量和时间约束的物流车路径
    public Map<Integer, Solution> buildInitialSolution(List<Customer> allCustomers,
                                                       List<List<Node>> busRoutes,
                                                       int maxTimeWindows,
                                                       Node distributionCenter) {
        Map<Integer, Solution> timeWindowSolutions = new HashMap<>();
        this.allCustomers = allCustomers;
        // 多时段处理，遍历每个时段。筛选时段客户
        for (int t = 1; t <= maxTimeWindows; t++) {
            Solution solution = new Solution(t);//创建解
            List<Customer> timeWindowCustomers = filterCustomersByTimeWindow(allCustomers, t);
            Set<Customer> finalBusCustomers = new HashSet<>();//初始化空集合finalBusCustomers
            for (Customer customer : timeWindowCustomers) { //筛选当前时段的公交服务客户
                if (customer.getNormalDemand() <= 0) {
                    continue;}
                // 检查该客户是否在任意一条公交路线的服务范围内
                boolean shouldBeServedByBus = false;
                for (List<Node> busRoute : busRoutes) {
                    if (customerClassifier.isOnBusRoute(customer, busRoute)) {
                        shouldBeServedByBus = true;
                        break; }}// 只要在一条路线上，就确定为公交客户
                if (shouldBeServedByBus) {
                    finalBusCustomers.add(customer);}
            }
            //公交车和物流车集合分类
            solution.setBusServiceCustomers(new HashSet<>());
            solution.setLogisticServiceCustomers(new HashSet<>());//初始化空集合
            System.out.println("\n=== 时段 " + t + " 客户分类开始 ===");
            System.out.println("该时段总客户数: " + timeWindowCustomers.size());
            System.out.println("公交车服务集合大小: " + finalBusCustomers.size());
            System.out.println("公交车服务客户ID: " +
                    finalBusCustomers.stream().map(Customer::getId).collect(java.util.stream.Collectors.toList()));

            for (Customer customer : timeWindowCustomers) {
                if (finalBusCustomers.contains(customer)) {
                    solution.addBusCustomer(customer);
                    System.out.println("  分配公交车: 客户" + customer.getId() +
                            " (普通需求: " + customer.getNormalDemand() + ")");
                } else {//不在公交车服务集合的客户点给物流车
                    solution.addLogisticCustomer(customer);
                }
            }
            //生成初始公交路径
            //清空旧路径，确保路径正确存入Solution
            solution.setBusRoutes(new ArrayList<>());
            for (List<Node> busRoute : busRoutes) {
                busRouteGenerator.generateInitialBusRoutes(solution, busRoute);}
            //调用busRouteGenerator生产初始公交车路径（不可行解）
            timeWindowSolutions.put(t, solution);}//存入映射表
        stationInserter.optimizeBusRoutesWithStations(timeWindowSolutions, allCustomers);
        //调用服务站插入器，对所有时段的公交路径做优化（核心是插入服务站，解决公交路径容量不足的问题）
        generateLogisticRoutes(timeWindowSolutions, distributionCenter);
        //调用物流路径生成逻辑，为所有时段的物流服务客户生成物流车路径（基于配送中心）
        return timeWindowSolutions;}
//调用相应方法，11.27没问题
    public void generateLogisticRoutes(Map<Integer, Solution> solutions, Node distributionCenter) {
        logisticRouteGenerator.generateLogisticRoutes(solutions, distributionCenter);
    }

//    public void reoptimizeBusRoutes(Solution solution) {
//        if (allCustomers != null) {
//            stationInserter.reoptimizeBusRoutes(solution, allCustomers);
//        }
//    }

    private List<Customer> filterCustomersByTimeWindow(List<Customer> allCustomers, int timeWindow) {
        List<Customer> filtered = new ArrayList<>();
        for (Customer customer : allCustomers) {
            if (customer.getRequiredTimeWindow() == timeWindow) {
                filtered.add(customer);
            }
        }
        return filtered;
    }
}
//busRouteGenerator.generateInitialBusRoutes(solution, busRoute) 生成初始公交路径，并把这些路径存入对应时段的 Solution 对象（solution.setBusRoutes(...)）；