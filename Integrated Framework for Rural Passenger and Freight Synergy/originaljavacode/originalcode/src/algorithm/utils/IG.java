package algorithm.utils;
import config.Parameters;
import model.*;
import java.util.*;
import java.util.stream.Collectors;
// 通用的迭代贪婪服务站工具类
public class IG {
    private Parameters parameters;
    private List<Node> candidateServiceStations;
    private Random random;

    public IG(List<Node> candidateServiceStations) {
        this.parameters = Parameters.getInstance();
        this.candidateServiceStations = candidateServiceStations;
        this.random = new Random();
    }

    public List<Route> processBusRoutes(Solution solution, List<Customer> allCustomers,
                                        boolean isFilterBusCustomers, boolean isFilterNodes) {
        List<Route> feasibleBusRoutes = new ArrayList<>();
        for (Route busRoute : solution.getBusRoutes()) {
            Route feasibleRoute = processSingleBusRoute(busRoute, solution, allCustomers, isFilterBusCustomers, isFilterNodes);
            feasibleBusRoutes.add(feasibleRoute);
        }
        return feasibleBusRoutes;
    }

    private Route processSingleBusRoute(Route initialRoute, Solution solution, List<Customer> allCustomers,
                                        boolean isFilterBusCustomers, boolean isFilterNodes) {
        List<Node> nodes = initialRoute.getNodes();
        if (isFilterNodes) {
            nodes = filterNodes(initialRoute, solution);
        }

        Route currentRoute = new Route(initialRoute.getVehicle(), initialRoute.getTimeWindow());
        double currentLoad = 0;
        int timeWindow = initialRoute.getTimeWindow();
        double busCapacity = parameters.getBusCapacity();

        List<Node> passedCandidateStations = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            Node currentNode = nodes.get(i);
            // 注释调试输出
            // System.out.println("\n处理节点 " + currentNode.getId() + " (索引=" + i + ")");

            if (isCandidateStation(currentNode)) {
                passedCandidateStations.add(currentNode);
                // 注释调试输出
                // System.out.println("  添加到passedCandidateStations，当前列表: " +
                //         passedCandidateStations.stream().map(Node::getId).collect(Collectors.toList()));
            }
            double nodeDemand = getNodeDemand(currentNode, timeWindow, allCustomers, solution, isFilterBusCustomers);
            // 注释调试输出
            // System.out.println("  需求=" + nodeDemand + ", 当前负载=" + currentLoad);
//11111111111111服务站的条件
            if (nodeDemand > 0) {
                if (currentLoad < nodeDemand) {//按照一定概率选择服务站
                    if (!passedCandidateStations.isEmpty()) {
//                        Node lastCandidate = passedCandidateStations.get(passedCandidateStations.size() - 1);
//                        //Node selectedCandidate = selectCandidateByProbability(passedCandidateStations);
//                        String stationId = lastCandidate.getId() + "S_T" + timeWindow +
//                                (isFilterBusCustomers ? "_ALNS" : "");
//                        ServiceStation station = new ServiceStation(
//                                stationId,
//                                lastCandidate.getX(),
//                                lastCandidate.getY(),
//                                busCapacity
//                        );
//                        // 调用修复后的防重复添加方法
//                        addServiceStationIfNotExist(solution, station);
//                        currentLoad = busCapacity;
//                        // 注释调试输出
//                        // System.out.println("  重置负载为: " + currentLoad);
//                    } else {
//                        // 注释调试输出
//                        // System.out.println("  无已走过的候选站，无法添加服务站！");
//                    }
//                }
//                currentRoute.addNode(currentNode, nodeDemand);
//                currentLoad -= nodeDemand;
//                // 注释调试输出
//                // System.out.println("  添加客户节点，负载更新为: " + currentLoad);
//            } else {
//                currentRoute.addNode(currentNode, 0);
//            }
//        }
//
//        // 注释调试输出
//        // System.out.println("最终路径: " + currentRoute.getNodes().stream().map(Node::getId).collect(Collectors.toList()));
//        // System.out.println("=== IG 处理路径 结束 ===\n");
//
//        return currentRoute;
//    }
                        Node selectedCandidate = selectCandidateByProbability(passedCandidateStations);
                        String stationId = selectedCandidate.getId() + "S_T" + timeWindow +
                                (isFilterBusCustomers ? "_ALNS" : "");
                        ServiceStation station = new ServiceStation(
                                stationId,
                                selectedCandidate.getX(),
                                selectedCandidate.getY(),
                                busCapacity
                        );
                        // 调用修复后的防重复添加方法
                        addServiceStationIfNotExist(solution, station);
                        currentLoad = busCapacity;
                        // 注释调试输出
                        // System.out.println("  重置负载为: " + currentLoad);
                    } else {
                        // 注释调试输出
                        // System.out.println("  无已走过的候选站，无法添加服务站！");
                    }
                }
                currentRoute.addNode(currentNode, nodeDemand);
                currentLoad -= nodeDemand;
                // 注释调试输出
                // System.out.println("  添加客户节点，负载更新为: " + currentLoad);
            } else {
                currentRoute.addNode(currentNode, 0);
            }
        }

        // 注释调试输出
        // System.out.println("最终路径: " + currentRoute.getNodes().stream().map(Node::getId).collect(Collectors.toList()));
        // System.out.println("=== IG 处理路径 结束 ===\n");

        return currentRoute;
    }
    // ========== 仅新增：概率选择方法（其余原有方法完全保留） ==========
    private Node selectCandidateByProbability(List<Node> passedCandidateStations) {
        int size = passedCandidateStations.size();
        // 生成0-99的随机数（对应0%-99%概率）
        int rand = random.nextInt(100);

        if (rand < 80) { // 80%选最后一个
            return passedCandidateStations.get(size - 1);
        } else if (rand < 95) { // 15%选倒数第二个（不足则选最后一个）
            return size >= 2 ? passedCandidateStations.get(size - 2) : passedCandidateStations.get(size - 1);
        } else { // 5%选倒数第三个（不足则选最后一个）
            return size >= 3 ? passedCandidateStations.get(size - 3) : passedCandidateStations.get(size - 1);
        }
    }


    private void addServiceStationIfNotExist(Solution solution, ServiceStation station) {
        // 1. 获取只读集合，仅用于判断是否存在（不修改）
        Set<ServiceStation> existingStations = solution.getServiceStations();
        // 2. 按ID判断是否已存在（避免重复添加）
        boolean isExist = existingStations.stream()
                .anyMatch(s -> s.getId().equals(station.getId()));
        if (!isExist) {
            solution.addServiceStation(station);
            // System.out.println("  → 服务站已添加到集合: " + station.getId() + "（未修改路径/节点）");
        } else {
            // System.out.println("  服务站已存在，跳过添加: " + station.getId());
        }
    }

    // 以下工具方法与之前一致，无需修改！
    private List<Node> filterNodes(Route initialRoute, Solution solution) {
        List<Node> cleaned = new ArrayList<>();
        for (Node node : initialRoute.getNodes()) {
            if (node != null) {
                cleaned.add(node);
            }
        }
        return cleaned;
    }

    private double getNodeDemand(Node node, int timeWindow, List<Customer> allCustomers,
                                 Solution solution, boolean isFilterBusCustomers) {
        if (!isFilterBusCustomers) {
            return getNodeDemandWithoutFilter(node, timeWindow, allCustomers);
        }
        Set<Customer> busCustomers = solution.getBusServiceCustomers();
        if (node instanceof Customer) {
            Customer customer = (Customer) node;
            if (customer.getRequiredTimeWindow() == timeWindow && busCustomers.contains(customer)) {
                return customer.getNormalDemand();
            }
            return 0.0;
        }
        for (Customer customer : allCustomers) {
            if (isSameLocation(node, customer) &&
                    customer.getRequiredTimeWindow() == timeWindow && busCustomers.contains(customer)) {
                return customer.getNormalDemand();
            }
        }
        return 0.0;
    }

    private double getNodeDemandWithoutFilter(Node node, int timeWindow, List<Customer> allCustomers) {
        if (node instanceof Customer) {
            Customer customer = (Customer) node;
            if (customer.getRequiredTimeWindow() == timeWindow) {
                return customer.getNormalDemand();
            }
            return 0.0;
        }
        for (Customer customer : allCustomers) {
            if (isSameLocation(node, customer) &&
                    customer.getRequiredTimeWindow() == timeWindow) {
                return customer.getNormalDemand();
            }
        }
        return 0.0;
    }

    private boolean isCandidateStation(Node node) {
        if (node == null || node.getId() == null) {
            return false;
        }
        for (Node candidate : candidateServiceStations) {
            if (candidate.getId().equals(node.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSameLocation(Node node1, Node node2) {
        double tolerance = 1e-6;
        return Math.abs(node1.getX() - node2.getX()) < tolerance &&
                Math.abs(node1.getY() - node2.getY()) < tolerance;
    }
}