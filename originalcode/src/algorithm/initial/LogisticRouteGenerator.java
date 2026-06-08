package algorithm.initial;
import model.*;
import algorithm.initial.savings.SavingsAlgorithm;
import config.Parameters;
import java.util.*;
import java.util.stream.Collectors;

public class LogisticRouteGenerator {
    private Parameters parameters;
    private SavingsAlgorithm savingsAlgorithm;

    public LogisticRouteGenerator() {
        this.parameters = Parameters.getInstance();
        this.savingsAlgorithm = new SavingsAlgorithm(parameters.getLogisticCapacity());
    }

    // 核心逻辑：为每个时段生成物流车路径
    public void generateLogisticRoutes(Map<Integer, Solution> solutions, Node distributionCenter) {
        for (Solution solution : solutions.values()) {//遍历所有时段方案
            System.out.println("\n=== 时段 " + solution.getTimeWindow() + " 物流车路径 ===");
            solution.setDistributionCenter(distributionCenter);
            // 获取物流服务节点（服务站+物流车顾客）
            Set<Node> logisticServiceNodes = solution.getLogisticServiceNodes();
            List<Node> validServiceNodes = new ArrayList<>();
            for (Node node : logisticServiceNodes) {
                if (node instanceof ServiceStation) {
                    ServiceStation station = (ServiceStation) node;
                    if (station.isIGGenerated()) {
                        validServiceNodes.add(node);
                    }
                } else if (node instanceof Customer) {
                    Customer customer = (Customer) node;
                    if (customer.getRequiredTimeWindow() == solution.getTimeWindow()) {
                        validServiceNodes.add(node);
                    }
                }
            }

            if (validServiceNodes.isEmpty()) {
                continue;
            }

            // 生成初始路径（不考虑车辆复用）
            List<Route> initialRoutes = generateInitialRoutes(validServiceNodes, distributionCenter, solution.getTimeWindow());

            List<VehicleSchedule> schedules = scheduleRoutesWithTime(initialRoutes, distributionCenter);

            List<Route> finalRoutes = extractRoutesFromSchedules(schedules);
            // 输出最终物流路径（初始解）
            System.out.println("调度后最终物流路径数量: " + finalRoutes.size());
            for (int i = 0; i < finalRoutes.size(); i++) {
                String finalRouteIds = finalRoutes.get(i).getNodes().stream()
                        .map(Node::getId)
                        .collect(Collectors.joining(", "));
                System.out.println("最终物流路径" + (i+1) + ": [" + finalRouteIds + "]");
            }


            solution.setLogisticRoutes(finalRoutes);
        }
    }

    protected List<Route> generateInitialRoutes(List<Node> serviceNodes, Node distributionCenter, int timeWindow) {
        List<Route> routes = new ArrayList<>();
        // 为每个节点创建独立路径
        for (Node node : serviceNodes) {
            Vehicle vehicle = new Vehicle(Vehicle.VehicleType.LOGISTIC, parameters.getLogisticCapacity());
            Route route = new Route(vehicle, timeWindow);
            double demand = getNodeDemand(node);
            route.addNode(distributionCenter, 0.0);
            route.addNode(node, demand);
            route.addNode(distributionCenter, 0.0);

            routes.add(route);
        }
        // 使用节约算法合并路径
        routes = savingsAlgorithm.generateRoutesWithTimeConstraint(serviceNodes, distributionCenter, timeWindow);

        return routes;
    }

    protected List<VehicleSchedule> scheduleRoutesWithTime(List<Route> routes, Node distributionCenter) {
        List<Vehicle> vehicles = createLogisticVehicles();
        List<VehicleSchedule> schedules = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            schedules.add(new VehicleSchedule(vehicle));
        }

        List<RouteTask> tasks = new ArrayList<>();
        for (Route route : routes) {
            RouteTask task = calculateRouteTask(route, distributionCenter);
            tasks.add(task);
        }

        // 按任务时间降序排序
        tasks.sort((t1, t2) -> Double.compare(t2.getTotalTime(), t1.getTotalTime()));

        // 分配任务到车辆
        for (RouteTask task : tasks) {
            boolean assigned = false;
            // 按当前时间排序，选择最早可用的车辆
            schedules.sort(Comparator.comparingDouble(VehicleSchedule::getCurrentTime));

            for (VehicleSchedule schedule : schedules) {
                if (schedule.canAddTask(task)) {
                    schedule.addTask(task);
                    assigned = true;
                    break;
                }
            }
        }

        return schedules;
    }


    protected RouteTask calculateRouteTask(Route route, Node distributionCenter) {
        List<Node> nodes = route.getNodes();
        double totalDistance = 0.0;
        double totalServiceTime = 0.0;

        // 计算总距离
        for (int i = 0; i < nodes.size() - 1; i++) {
            Node from = nodes.get(i);
            Node to = nodes.get(i + 1);
            totalDistance += calculateDistance(from, to);
        }

        // 计算服务时间（不包括配送中心）
        for (int i = 1; i < nodes.size() - 1; i++) {
            Node node = nodes.get(i);
            if (node.getType() == Node.NodeType.SERVICE_STATION) {
                totalServiceTime += parameters.getStationServiceTime();
            } else if (node instanceof Customer) {
                totalServiceTime += parameters.getCustomerServiceTime();
            }
        }

        double travelTime = totalDistance / parameters.getVehicleSpeed();
        double totalTime = travelTime + totalServiceTime;

        return new RouteTask(route, travelTime, totalServiceTime);
    }


    protected List<Route> extractRoutesFromSchedules(List<VehicleSchedule> schedules) {
        List<Route> finalRoutes = new ArrayList<>();

        for (VehicleSchedule schedule : schedules) {
            for (RouteTask task : schedule.getTasks()) {
                Route route = task.getRoute();
                Route newRoute = new Route(schedule.getVehicle(), route.getTimeWindow());
                newRoute.setNodesWithDemands(route.getNodes(), route.getDemands());
                finalRoutes.add(newRoute);
            }
        }

        return finalRoutes;
    }


    private List<Vehicle> createLogisticVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();
        int vehicleCount = parameters.getLogisticVehicleCount();

        for (int i = 1; i <= vehicleCount; i++) {
            Vehicle vehicle = new Vehicle(Vehicle.VehicleType.LOGISTIC, parameters.getLogisticCapacity());
            vehicle.setId("LOG_VEHICLE_" + i);
            vehicles.add(vehicle);
        }

        return vehicles;
    }


    private double calculateDistance(Node from, Node to) {
        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }


    protected double getNodeDemand(Node node) {
        if (node instanceof ServiceStation) {
            return ((ServiceStation) node).getReplenishmentDemand();
        } else if (node instanceof Customer) {
            return ((Customer) node).getTotalDemand();
        }
        return 0.0;
    }
}