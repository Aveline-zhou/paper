package algorithm.initial.savings;

import config.Parameters;
import model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 节约算法主逻辑 - 实现完整的Clark and Wright节约算法（新增时间约束支持）
 */
public class SavingsAlgorithm {
    private final double vehicleCapacity;

    public SavingsAlgorithm(double vehicleCapacity) {
        this.vehicleCapacity = vehicleCapacity;
    }

    /**
     * 原有执行方法 - 保持不变
     */
    public List<Route> execute(List<Node> serviceNodes, Node distributionCenter, int timeWindow) {
        if (serviceNodes.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 1: 创建初始路径
        List<Route> routes = createInitialRoutes(serviceNodes, distributionCenter, timeWindow);

        // Step 2: 计算节约值
        List<Savings> savingsList = SavingsCalculator.calculateAllSavings(serviceNodes, distributionCenter);

        // Step 3: 按节约值降序排序
        Collections.sort(savingsList);

        // Step 4: 依次处理节约值，合并路径
        int mergeCount = 0;
        for (Savings savings : savingsList) {
            Node nodeI = savings.getNodeI();
            Node nodeJ = savings.getNodeJ();

            Route routeI = findRouteContainingNode(routes, nodeI);
            Route routeJ = findRouteContainingNode(routes, nodeJ);

            if (isValidForMerge(routeI, routeJ, nodeI, nodeJ)) {
                Route mergedRoute = RouteMerger.mergeRoutes(routeI, routeJ, nodeI, nodeJ, timeWindow, vehicleCapacity);
                routes.remove(routeI);
                routes.remove(routeJ);
                routes.add(mergedRoute);
                mergeCount++;
            }
        }

        return routes;
    }

    /**
     * 🔥 新增：带时间约束的节约算法执行方法
     */
    public List<Route> generateRoutesWithTimeConstraint(List<Node> serviceNodes,
                                                        Node distributionCenter,
                                                        int timeWindow) {
        if (serviceNodes.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 1: 创建初始路径（复用原有方法）
        List<Route> routes = createInitialRoutes(serviceNodes, distributionCenter, timeWindow);

        // Step 2: 计算节约值（复用原有工具类）
        List<Savings> savingsList = SavingsCalculator.calculateAllSavings(serviceNodes, distributionCenter);

        // Step 3: 按节约值降序排序（复用原有排序逻辑）
        Collections.sort(savingsList);

        // Step 4: 合并路径（新增时间约束检查）
        int mergeCount = 0;
        for (Savings savings : savingsList) {
            Node nodeI = savings.getNodeI();
            Node nodeJ = savings.getNodeJ();

            // 查找包含目标节点的路径（复用原有方法）
            Route routeI = findRouteContainingNode(routes, nodeI);
            Route routeJ = findRouteContainingNode(routes, nodeJ);

            if (isValidForMerge(routeI, routeJ, nodeI, nodeJ)) {
                Route mergedRoute = RouteMerger.mergeRoutes(routeI, routeJ, nodeI, nodeJ, timeWindow, vehicleCapacity);
                if (satisfiesTimeConstraint(mergedRoute, distributionCenter)) {
                    routes.remove(routeI);
                    routes.remove(routeJ);
                    routes.add(mergedRoute);
                    mergeCount++;
                }
            }
        }

        return routes;
    }

    /**
     * 🔥 新增：检查路径是否满足时间约束
     */
    private boolean satisfiesTimeConstraint(Route route, Node distributionCenter) {
        Parameters params = Parameters.getInstance();
        List<Node> nodes = route.getNodes();

        // 计算总距离
        double totalDistance = 0.0;
        for (int i = 0; i < nodes.size() - 1; i++) {
            totalDistance += calculateDistance(nodes.get(i), nodes.get(i + 1));
        }

        // 计算服务时间
        double totalServiceTime = 0.0;
        for (int i = 1; i < nodes.size() - 1; i++) {
            Node node = nodes.get(i);
            if (node.getType() == Node.NodeType.SERVICE_STATION) {
                totalServiceTime += params.getStationServiceTime();
            } else if (node instanceof Customer) {
                totalServiceTime += params.getCustomerServiceTime();
            }
        }

        // 计算总时间（行驶时间+服务时间）
        double travelTime = totalDistance / params.getVehicleSpeed();
        double totalTime = travelTime + totalServiceTime;

        // 检查是否超过最大工作时间
        return totalTime <= params.getMaxWorkingTime();
    }

    /**
     * 🔥 新增：计算路径总耗时（辅助方法）
     */
    private double calculateRouteTotalTime(Route route, Node distributionCenter) {
        Parameters params = Parameters.getInstance();
        List<Node> nodes = route.getNodes();

        double totalDistance = 0.0;
        for (int i = 0; i < nodes.size() - 1; i++) {
            totalDistance += calculateDistance(nodes.get(i), nodes.get(i + 1));
        }

        double totalServiceTime = 0.0;
        for (int i = 1; i < nodes.size() - 1; i++) {
            Node node = nodes.get(i);
            if (node.getType() == Node.NodeType.SERVICE_STATION) {
                totalServiceTime += params.getStationServiceTime();
            } else if (node instanceof Customer) {
                totalServiceTime += params.getCustomerServiceTime();
            }
        }

        return (totalDistance / params.getVehicleSpeed()) + totalServiceTime;
    }

    /**
     * 🔥 新增：计算两点间欧几里得距离（复用已有逻辑）
     */
    private double calculateDistance(Node from, Node to) {
        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 原有方法 - 保持不变
     */
    private List<Route> createInitialRoutes(List<Node> serviceNodes, Node distributionCenter, int timeWindow) {
        List<Route> routes = new ArrayList<>();

        for (Node node : serviceNodes) {
            Route route = new Route(new Vehicle(Vehicle.VehicleType.LOGISTIC, vehicleCapacity), timeWindow);
            route.addNode(distributionCenter, 0);
            route.addNode(node, getNodeDemand(node));
            route.addNode(distributionCenter, 0);
            routes.add(route);
        }

        return routes;
    }

    /**
     * 原有方法 - 保持不变
     */
    private Route findRouteContainingNode(List<Route> routes, Node targetNode) {
        for (Route route : routes) {
            for (Node node : route.getNodes()) {
                // 通过ID比较而不是对象引用比较
                if (node.getId().equals(targetNode.getId())) {
                    return route;
                }
            }
        }
        return null;
    }

    /**
     * 原有方法 - 保持不变
     */
    private boolean isValidForMerge(Route routeI, Route routeJ, Node nodeI, Node nodeJ) {
        if (routeI == null || routeJ == null || routeI.equals(routeJ)) {
            return false;
        }

        // 检查是否可以合并（容量约束等）
        return RouteMerger.canMergeRoutes(routeI, routeJ, nodeI, nodeJ, vehicleCapacity);
    }

    /**
     * 原有方法 - 保持不变
     */
    private double getNodeDemand(Node node) {
        if (node instanceof Customer) {
            return ((Customer) node).getTotalDemand();
        } else if (node instanceof ServiceStation) {
            // 关键修复：服务站的需求是补货需求
            return ((ServiceStation) node).getReplenishmentDemand();
        }
        return 0;
    }
}