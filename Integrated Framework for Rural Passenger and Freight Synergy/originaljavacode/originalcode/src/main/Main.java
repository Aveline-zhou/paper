package main;

import model.*;
import algorithm.initial.InitialSolutionBuilder;
import algorithm.ALNSOptimizer;
import algorithm.utils.SolutionEvaluator;
import config.Parameters;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private static Map<Integer, Node> nodesMap = new HashMap<>();
    private static List<Customer> allCustomers = new ArrayList<>();
    private static List<List<Node>> busRoutes = new ArrayList<>();
    private static Node distributionCenter;
    private static List<Node> candidateServiceStations = new ArrayList<>();
    private static Map<Integer, Node> busStopsMap = new HashMap<>();

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();  // 开始时间
        try {
            initializeParameters();
             String excelFilePath ="D:\\.xlsx";
            if (!loadDataFromExcel(excelFilePath)) {
                return;
            }
            // 构建初始解
            System.out.println("\n=== 开始构建初始解 ===");
            InitialSolutionBuilder initialBuilder = new InitialSolutionBuilder(candidateServiceStations);
            Parameters parameters = Parameters.getInstance();

            Map<Integer, Solution> initialSolutions = initialBuilder.buildInitialSolution(
                    allCustomers, busRoutes, parameters.getMaxTimeWindows(),
                    distributionCenter);

            // 验证初始解
            System.out.println("\n=== 初始解验证 ===");
            SolutionEvaluator evaluator = new SolutionEvaluator(parameters);
            for (Map.Entry<Integer, Solution> entry : initialSolutions.entrySet()) {
                Solution solution = entry.getValue();
                // double cost = evaluator.evaluateTotalCost(solution);
                int timeWindow = entry.getKey();
                double totalCost = evaluator.evaluateTotalCost(solution);
                double busServiceCost = evaluator.calculateBusServiceCost(solution);
                double logisticTransportCost = evaluator.calculateLogisticTransportCost(solution);

                double stationCost = evaluator.calculateStationConstructionCost(solution);
                double inventoryCost = evaluator.calculateInventoryCost(solution);

                System.out.println("===== 初始解 - 时段 " + timeWindow + " 成本（简短版） =====");
   //             System.out.printf("  公交服务成本: %.2f%n", busServiceCost);
   //             System.out.printf("  物流运输成本: %.2f%n", logisticTransportCost);
 //               System.out.printf("  服务站建设成本: %.2f%n", stationCost);
                System.out.printf("  总成本: %.2f%n", totalCost);
                System.out.println("===============================================\n");
            }

            // ALNS优化
            System.out.println("\n=== 开始ALNS优化 ===");
            ALNSOptimizer optimizer = new ALNSOptimizer(
                    parameters.getInitialTemperature(),
                    parameters.getCoolingRate(),
                    parameters.getEndTemperature(),
                    parameters.getMaxIterations(),
                    allCustomers,
                    distributionCenter,
                    initialBuilder,
                    candidateServiceStations
            );
            Map<Integer, Solution> optimizedSolutions = optimizer.optimizeMultiple(initialSolutions);
            ensureLogisticRoutesGenerated(optimizedSolutions, distributionCenter);
            outputResults(optimizedSolutions, evaluator);


        } catch (Exception e) {
            // 保留顶层异常捕获，用于处理算法执行等其他部分的严重错误
            System.err.println("❌ 程序执行出错");
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();    // 结束时间
        long totalTime = endTime - startTime;

        System.out.println("总运行时间: " + totalTime + " 毫秒");
        System.out.println("总运行时间: " + (totalTime / 1000.0) + " 秒");
    }
    private static boolean loadDataFromExcel(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            // 读取四个工作表
            if (!readNodesTable(workbook)) return false;
            if (!readCustomerDemandsTable(workbook)) return false;
            if (!readBusRoutesTable(workbook)) return false;
            if (!readCandidateServiceStationsTable(workbook)) return false;

            // 设置配送中心
            distributionCenter = nodesMap.get(0);
            if (distributionCenter == null) return false;
            return true;

        } catch (Exception e) {
            return false;
        }
    }
    private static boolean readNodesTable(Workbook workbook) {
        try {
            Sheet sheet = workbook.getSheet("Nodes");
            if (sheet == null) {
                return false;}
            nodesMap.clear();
            busStopsMap.clear();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {int nodeId = (int) getNumericCellValue(row.getCell(0));
                    double x = getNumericCellValue(row.getCell(1));
                    double y = getNumericCellValue(row.getCell(2));
                    String typeStr = getStringCellValue(row.getCell(3));

                    Node.NodeType type = Node.NodeType.valueOf(typeStr.toUpperCase());
                    Node node = new Node(String.valueOf(nodeId), x, y, type);
                    nodesMap.put(nodeId, node);

                    if (type == Node.NodeType.BUS_STOP) {
                        busStopsMap.put(nodeId, node);
                    }} catch (Exception e) {
                    return false;}}
            return true;} catch (Exception e) {return false;}}

    private static boolean readCustomerDemandsTable(Workbook workbook) {
        try {
            Sheet sheet = workbook.getSheet("CustomerDemands");
            if (sheet == null) return false;
            allCustomers.clear();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                int NodeId = (int) getNumericCellValue(row.getCell(0));
                double normalDemand = getNumericCellValue(row.getCell(1));
                double specialDemand = getNumericCellValue(row.getCell(2));
                int timeWindow = (int) getNumericCellValue(row.getCell(3));
                Node baseNode = nodesMap.get(NodeId); // 通过数字ID查找
                if (baseNode == null) continue;
                String customerId = NodeId + "T" + timeWindow;
                allCustomers.add(new Customer(customerId, baseNode.getX(), baseNode.getY(),
                        normalDemand, specialDemand, timeWindow));
            }

            System.out.println("读取CustomerDemands表: " + allCustomers.size() + " 个客户需求");
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    private static boolean readBusRoutesTable(Workbook workbook) {
        try {
            Sheet sheet = workbook.getSheet("BusRoutes");
            if (sheet == null) return false;
            busRoutes.clear();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                int startStop = (int)getNumericCellValue(row.getCell(1));
                int endStop =(int)getNumericCellValue(row.getCell(2));
                String intermediateStopsStr = getStringCellValue(row.getCell(3));
                List<Node> route = new ArrayList<>();
                Node startNode = busStopsMap.get(startStop);//起点
                if (startNode == null) continue;
                route.add(startNode);

                if (intermediateStopsStr != null && !intermediateStopsStr.trim().isEmpty()) {
                    String[] intermediateStops = intermediateStopsStr.split(";");
                    for (String stopIdStr : intermediateStops) {
                        int stopId = Integer.parseInt(stopIdStr.trim());
                        Node stopNode = nodesMap.get(stopId);
                        if (stopNode != null) {
                            route.add(stopNode);
                        }
                    }
                }
                Node endNode = busStopsMap.get(endStop);
                if (endNode == null) continue;
                route.add(endNode);
                busRoutes.add(route);
            }

            System.out.println("读取BusRoutes表: " + busRoutes.size() + " 条公交路线");
            return true;

        } catch (Exception e) {
            // 移除具体的表读取错误信息
            return false;
        }
    }

    private static boolean readCandidateServiceStationsTable(Workbook workbook) {
        try {
            Sheet sheet = workbook.getSheet("CandidateServiceStations");
            if (sheet == null) return false;
            candidateServiceStations.clear(); // 清空旧数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                int stationId = (int) getNumericCellValue(row.getCell(0));  // 数字ID
                Node stationNode = nodesMap.get(stationId);
                if (stationNode != null) {
                    candidateServiceStations.add(stationNode);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    private static double getNumericCellValue(Cell cell) {
        if (cell == null) return 0.0;
        switch (cell.getCellType()) {
            case NUMERIC: return cell.getNumericCellValue();
            case STRING:
                try { return Double.parseDouble(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { return 0.0; }
            default: return 0.0;
        }
    }
    private static String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((int) cell.getNumericCellValue());
            default: return "";
        }
    }
    private static void ensureLogisticRoutesGenerated(Map<Integer, Solution> solutions, Node distributionCenter) {
        for (Solution solution : solutions.values()) {
            if (solution.getLogisticServiceCustomers().size() > 0 &&
                    solution.getLogisticRoutes().size() == 0) {
                solution.setLogisticRoutes(generateLogisticRoutesDirectly(solution, distributionCenter));
            }
        }
    }
    private static List<Route> generateLogisticRoutesDirectly(Solution solution, Node distributionCenter) {
        List<Route> routes = new ArrayList<>();
        Parameters parameters = Parameters.getInstance();

        for (Customer customer : solution.getLogisticServiceCustomers()) {
            Route route = new Route(
                    new Vehicle(Vehicle.VehicleType.LOGISTIC, parameters.getLogisticCapacity()),
                    solution.getTimeWindow()
            );
            route.addNode(distributionCenter, 0.0);
            route.addNode(customer, customer.getTotalDemand());
            route.addNode(distributionCenter, 0.0);
            routes.add(route);
        }

        for (Node node : solution.getServiceStations()) {
            if (node instanceof ServiceStation) {
                ServiceStation station = (ServiceStation) node;
                Route route = new Route(
                        new Vehicle(Vehicle.VehicleType.LOGISTIC, parameters.getLogisticCapacity()),
                        solution.getTimeWindow()
                );
                route.addNode(distributionCenter, 0.0);
                route.addNode(station, station.getReplenishmentDemand());
                route.addNode(distributionCenter, 0.0);
                routes.add(route);
            }
        }
        return routes;
    }
    //初始化参数
    private static void initializeParameters() {
        Parameters parameters = Parameters.getInstance();
        parameters.initializeDefaultParameters();
        System.out.println("=== 参数配置初始化完成 ===");
    }
    private static void outputResults(Map<Integer, Solution> solutions, SolutionEvaluator evaluator) {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("=== 最终优化结果详情 ===");

        double totalCost = 0;
        // 新增：统计全时段除服务站建设成本外的总成本
        double totalOtherCostsSum = 0;
        for (Map.Entry<Integer, Solution> entry : solutions.entrySet()) {
            int timeWindow = entry.getKey();
            Solution solution = entry.getValue();
            System.out.println("\n--- 时段 " + timeWindow + " 详情 ---");

////            // 服务分配统计
//            System.out.println("服务分配统计:");
//            System.out.println("  公交服务客户数: " + solution.getBusServiceCustomers().size());
//
//
//            System.out.println("  服务站数量: " + solution.getServiceStations().size());
//            // 服务站详情
//            System.out.println("\n🔧 服务站详情 (" + solution.getServiceStations().size() + "个):");
            if (solution.getServiceStations().isEmpty()) {
                System.out.println("  无服务站");
            } else {
                int stationIndex = 1;
                for (Node station : solution.getServiceStations()) {
                    if (station instanceof ServiceStation) {
                        ServiceStation serviceStation = (ServiceStation) station;
                        System.out.println("  服务站 " + stationIndex + ": " + serviceStation.getId() +
                                " 位置:(" + serviceStation.getX() + "," + serviceStation.getY() + ")" +
                                " 补货需求:" + serviceStation.getReplenishmentDemand());
                        stationIndex++;
                    }
                }
            }


            // 物流车路径详情
            System.out.println("\n🚚物流车路径详情 (" + solution.getLogisticRoutes().size() + "条):");
            if (solution.getLogisticRoutes().isEmpty()) {
                // System.out.println("  无物流车路径!");
            } else {
                for (int i = 0; i < solution.getLogisticRoutes().size(); i++) {
                    Route logisticRoute = solution.getLogisticRoutes().get(i);
                    // 只输出路径编号和物流车ID，移除所有详情输出
                    System.out.println("  路径 " + (i+1) + " -物流车编号: " + logisticRoute.getId());
                    // 移除了所有关于节点序列、总需求、路径成本和详细节点的输出代码
//                    System.out.println();
                }
            }


            // 计算成本项（简短输出）
            double solutionCost = evaluator.evaluateTotalCost(solution);
            double busServiceCost = evaluator.calculateBusServiceCost(solution);
            double logisticTransportCost = evaluator.calculateLogisticTransportCost(solution);
            double stationCost = evaluator.calculateStationConstructionCost(solution);
            double inventoryCost = evaluator.calculateInventoryCost(solution);
            double otherCostsSum = busServiceCost + logisticTransportCost + inventoryCost;

            totalCost += solutionCost;
            totalOtherCostsSum += otherCostsSum;
        }

        System.out.println("\n" + "=".repeat(100));
        System.out.printf("💰 全时段总系统成本: %.2f%n", totalCost);
        System.out.println("=".repeat(100));
    }


    private static String getCustomerDetailedInfo(Customer customer) {
        return String.format("客户%s 位置(%.1f,%.1f) 需求[普通:%.1f 特殊:%.1f 总:%.1f] 实际配送时段:%d 服务方式:%s",
                customer.getId(),
                customer.getX(),
                customer.getY(),
                customer.getNormalDemand(),
                customer.getSpecialDemand(),
                customer.getTotalDemand(),
                customer.getActualDeliveryTime(),
                customer.isServedByBus() ? "公交车" : "物流车");
    }


    private static String formatRouteNodes(Route route) {
        return route.getNodes().stream()
                .map(node -> {
                    if (node instanceof Customer) {
                        Customer cust = (Customer) node;
                        return cust.getId() + "(普:" + cust.getNormalDemand() + ",特:" + cust.getSpecialDemand() + ")";
                    } else if (node instanceof ServiceStation) {
                        ServiceStation station = (ServiceStation) node;
                        return "服务站@" + station.getId() + "(需:" + station.getReplenishmentDemand() + ")";
                    } else {
                        return node.getId();
                    }
                })
                .collect(Collectors.joining(" → "));
    }


    private static String getNodeDetailedInfo(Node node, double demand) {
        if (node instanceof Customer) {
            Customer customer = (Customer) node;
            return String.format("客户%s 位置(%.1f,%.1f) 需求[普通:%.1f 特殊:%.1f 总:%.1f] 时段:%d 实际配送时段:%d 服务方式:%s",
                    customer.getId(), customer.getX(), customer.getY(),
                    customer.getNormalDemand(), customer.getSpecialDemand(), customer.getTotalDemand(),
                    customer.getRequiredTimeWindow(),
                    customer.getActualDeliveryTime(),
                    customer.isServedByBus() ? "公交车" : "物流车");
        } else if (node instanceof ServiceStation) {
            ServiceStation station = (ServiceStation) node;
            return String.format("服务站%s 位置(%.1f,%.1f) 补货需求:%.1f",
                    station.getId(), station.getX(), station.getY(), station.getReplenishmentDemand());
        } else {
            return String.format("%s 位置(%.1f,%.1f) 类型:%s",
                    node.getId(), node.getX(), node.getY(), node.getType().toString());
        }
    }

}