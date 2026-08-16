package model;

import algorithm.utils.SolutionEvaluator;
import config.Parameters;
import java.util.*;

public class Solution {
    private int timeWindow;
    private List<Route> busRoutes;
    private List<Route> logisticRoutes;
    private Set<ServiceStation> serviceStations;
    private Set<Customer> busServiceCustomers;
    private Set<Customer> logisticServiceCustomers;
    private double totalCost;
    private SolutionEvaluator evaluator;
    private Node distributionCenter;

    public Solution(int timeWindow) {
        this.timeWindow = timeWindow;
        this.busRoutes = new ArrayList<>();
        this.logisticRoutes = new ArrayList<>();
        this.serviceStations = new HashSet<>();
        this.busServiceCustomers = new HashSet<>();
        this.logisticServiceCustomers = new HashSet<>();
        this.totalCost = 0.0;
        this.evaluator = new SolutionEvaluator(Parameters.getInstance());
        this.distributionCenter = null;
    }

    public Solution(Solution other) {
        this.timeWindow = other.timeWindow;

        // 深拷贝路径
        this.busRoutes = new ArrayList<>();
        for (Route route : other.busRoutes) {
            this.busRoutes.add(copyRoute(route));
        }
        this.logisticRoutes = new ArrayList<>();
        for (Route route : other.logisticRoutes) {
            this.logisticRoutes.add(copyRoute(route));
        }

        // 深拷贝集合
        this.serviceStations = new HashSet<>(other.serviceStations);
        this.busServiceCustomers = new HashSet<>(other.busServiceCustomers);
        this.logisticServiceCustomers = new HashSet<>(other.logisticServiceCustomers);

        this.totalCost = other.totalCost;
        this.evaluator = new SolutionEvaluator(Parameters.getInstance());
        this.distributionCenter = other.distributionCenter;
    }

    private Route copyRoute(Route original) {
        Route copy = new Route(original.getVehicle(), original.getTimeWindow());
        copy.setNodesWithDemands(original.getNodes(), original.getDemands());
        copy.setTotalCost(original.getTotalCost());
        copy.setTotalDemand(original.getTotalDemand());
        return copy;
    }

    /**
     * 复制方法，带有调试日志
     */
    public Solution copy() {
        Solution newCopy = new Solution(this);

        // System.out.println("    - 复制后: 公交顾客=" + newCopy.getBusServiceCustomers().size() + ", 物流顾客=" + newCopy.getLogisticServiceCustomers().size());

        // 检查关键数据是否复制成功
        if (this.getLogisticServiceCustomers().size() > 0 && newCopy.getLogisticServiceCustomers().size() == 0) {
            // System.err.println("    !!!! 严重错误: 复制过程中物流顾客信息丢失 !!!!");
        }

        return newCopy;
    }

    public boolean addBusCustomer(Customer customer) {
        logisticServiceCustomers.remove(customer);
        boolean added = busServiceCustomers.add(customer);
        if (added) {
            customer.setServedByBus(true);
            updateTotalCost();
        }
        return added;
    }

    public boolean addLogisticCustomer(Customer customer) {
        // 1. 先从公交车服务集合中移除（避免重复）
        busServiceCustomers.remove(customer);
        boolean added = logisticServiceCustomers.add(customer);
        if (added) {
            customer.setServedByBus(false);
            updateTotalCost();
        }
        return added;
    }
    public boolean removeCustomer(Customer customer) {
        // 1. 从两个集合中尝试移除
        boolean removedFromBus = busServiceCustomers.remove(customer);
        boolean removedFromLogistic = logisticServiceCustomers.remove(customer);
        // 2. 只要从任一集合移除成功，就更新状态并计算成本
        if (removedFromBus || removedFromLogistic) {
            customer.setServedByBus(false);
            updateTotalCost();
        }
        return removedFromBus || removedFromLogistic;
    }

    public void validateServiceMode() {

        // 1. 检查是否有顾客同时出现在两个服务集合（重复服务）
        Set<Customer> duplicateCustomers = new HashSet<>(busServiceCustomers);
        duplicateCustomers.retainAll(logisticServiceCustomers);
        if (!duplicateCustomers.isEmpty()) {
            // System.err.println("⚠️ 错误: 以下顾客同时被公交车和物流车服务（重复服务）:");
            for (Customer c : duplicateCustomers) {
                // System.err.println("  - 顾客ID: " + c.getId() + " (坐标: (" + c.getX() + "," + c.getY() + "))");
            }
        } else {

        }

        // 2. 检查公交车集合中顾客的状态是否为 "公交服务"
        boolean busStatusError = false;
        for (Customer c : busServiceCustomers) {
            if (!c.isServedByBus()) {
                // System.err.println("⚠️ 错误: 顾客 " + c.getId() + " 在公交车服务集合中，但服务状态为「物流车」");
                busStatusError = true;
            }
        }
        if (!busStatusError) {

        }

        // 3. 检查物流车集合中顾客的状态是否为 "物流服务"
        boolean logisticStatusError = false;
        for (Customer c : logisticServiceCustomers) {
            if (c.isServedByBus()) {
                // System.err.println("⚠️ 错误: 顾客 " + c.getId() + " 在物流车服务集合中，但服务状态为「公交车」");
                logisticStatusError = true;
            }
        }
        if (!logisticStatusError) {

        }

        // System.out.println("=== 服务模式一致性验证结束 ===");
    }

    public void setDistributionCenter(Node distributionCenter) {
        this.distributionCenter = distributionCenter;
    }

    public Node getDistributionCenter() {
        if (this.distributionCenter == null) {
            // System.err.println("警告：配送中心未设置！");
        }
        return this.distributionCenter;
    }

    public void clearServiceStations() {
        this.serviceStations.clear();
        updateTotalCost();
    }

    public void addBusRoute(Route route) {
        busRoutes.add(route);
        updateTotalCost();
    }

    public void addLogisticRoute(Route route) {
        logisticRoutes.add(route);
        updateTotalCost();
    }

    public void addServiceStation(ServiceStation station) {
        this.serviceStations.add(station);
        updateTotalCost();
    }

    public List<Customer> getAllCustomers() {
        List<Customer> all = new ArrayList<>();
        all.addAll(busServiceCustomers);
        all.addAll(logisticServiceCustomers);
        return all;
    }

    public Set<Node> getLogisticServiceNodes() {
        Set<Node> nodes = new HashSet<>();
        nodes.addAll(logisticServiceCustomers);
        nodes.addAll(serviceStations);
        return nodes;
    }

    private void updateTotalCost() {
        this.totalCost = evaluator.evaluateTotalCost(this);
    }

    public void setLogisticRoutes(List<Route> logisticRoutes) {
        this.logisticRoutes = new ArrayList<>(logisticRoutes);
        updateTotalCost();
    }

    private boolean containsServiceableNodes(Route route) {
        for (Node node : route.getNodes()) {
            if (node instanceof Customer || node instanceof ServiceStation) {
                return true;
            }
        }
        return false;
    }

    public void setBusRoutes(List<Route> busRoutes) {
        this.busRoutes = new ArrayList<>(busRoutes);
        updateTotalCost();
    }

    public void setServiceStations(Set<ServiceStation> serviceStations) {
        this.serviceStations = new HashSet<>(serviceStations);
        updateTotalCost();
    }

    public void setEvaluator(SolutionEvaluator evaluator) {
        this.evaluator = evaluator;
        updateTotalCost();
    }

    // Getters
    public int getTimeWindow() {
        return timeWindow;
    }

    public List<Route> getBusRoutes() {
        return new ArrayList<>(busRoutes);
    }

    public List<Route> getLogisticRoutes() {
        List<Route> valid = new ArrayList<>();
        for (Route r : logisticRoutes) {
            if (containsServiceableNodes(r)) valid.add(r);
        }
        return valid;
    }

    public Set<ServiceStation> getServiceStations() {
        return Collections.unmodifiableSet(serviceStations);
    }

    public Set<Customer> getBusServiceCustomers() {
        return new HashSet<>(busServiceCustomers);
    }

    public Set<Customer> getLogisticServiceCustomers() {
        return new HashSet<>(logisticServiceCustomers);
    }

    public double getTotalCost() {
        updateTotalCost();
        return totalCost;
    }

    // Setters
    public void setTimeWindow(int timeWindow) {
        this.timeWindow = timeWindow;
    }

    public void setBusServiceCustomers(Set<Customer> busServiceCustomers) {
        this.busServiceCustomers = new HashSet<>(busServiceCustomers);
        updateTotalCost();
    }

    public void setLogisticServiceCustomers(Set<Customer> logisticServiceCustomers) {
        this.logisticServiceCustomers = new HashSet<>(logisticServiceCustomers);
        updateTotalCost();
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    // 在Solution类中添加
    public void clearLogisticRoutes() {
        this.logisticRoutes.clear();
    }

    public void printSolutionDetails() {
         System.out.println("=== 解决方案详情 ===");
         System.out.println("时段: " + timeWindow);
         System.out.println("公交车服务顾客数: " + busServiceCustomers.size());
         System.out.println("物流车服务顾客数: " + logisticServiceCustomers.size());
         System.out.println("服务站数量: " + serviceStations.size());
         System.out.println("公交车路径数: " + busRoutes.size());
         System.out.println("物流车路径数: " + getLogisticRoutes().size());
         System.out.println("总成本: " + totalCost);
         System.out.println("======================");
    }
}