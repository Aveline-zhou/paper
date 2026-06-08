package algorithm;

import algorithm.initial.InitialSolutionBuilder;
import algorithm.utils.LGnewRoute;
import algorithm.utils.ProbabilityCalculator;
import algorithm.utils.ReoptimizeBusRoutes;
import model.*;
import algorithm.operators.*;
import algorithm.utils.SolutionEvaluator;
import config.Parameters;

import java.util.*;

public class ALNSOptimizer {
    private double initialTemperature;
    private double coolingRate;
    private double endTemperature;
    private int maxIterations;
    private int segmentLength;
    private Parameters parameters;
    private List<Customer> allCustomers;
    private Node distributionCenter;
    private InitialSolutionBuilder solutionBuilder;
    private OperatorManager operatorManager;
    private TimeWindowIndependentOperator timeWindowIndependentOperator;//111
    private ReoptimizeBusRoutes reoptimizeBusRoutesTool;
    private List<Node> candidateServiceStations;
    private double independentProbability = 0.2;


    public ALNSOptimizer(double initialTemp, double coolingRate, double endTemp, int maxIter,
                         List<Customer> customers, Node distributionCenter,
                         InitialSolutionBuilder solutionBuilder,
                         List<Node> candidateServiceStations) {
        this.initialTemperature = initialTemp;
        this.coolingRate = coolingRate;
        this.endTemperature = endTemp;
        this.maxIterations = maxIter;
        this.segmentLength = 100;
        this.allCustomers = customers;
        this.distributionCenter = distributionCenter;
        this.solutionBuilder = solutionBuilder;
        this.parameters = Parameters.getInstance();
        this.operatorManager = new OperatorManager(this.segmentLength);

        this.candidateServiceStations = candidateServiceStations;
        this.reoptimizeBusRoutesTool = new ReoptimizeBusRoutes(candidateServiceStations);
        this.timeWindowIndependentOperator = new TimeWindowIndependentOperator();
        timeWindowIndependentOperator.setAdjustmentCount(0);

        System.out.println(" ALNSOptimizer 初始化完成");
    }

    // ==================== 单时段优化方法（保留原有功能） ====================
    public Solution optimize(Solution initialSolution) {
        System.out.println("=== 开始单时段ALNS优化 ===");
        Solution currentSolution = makeSolutionFeasible(initialSolution.copy());
        Solution bestSolution = currentSolution.copy();

        SolutionEvaluator evaluator = new SolutionEvaluator(parameters);

        double initialCost = evaluator.evaluateTotalCost(currentSolution);
        currentSolution.setTotalCost(initialCost);
        bestSolution.setTotalCost(initialCost);

        System.out.println("初始可行解状态:");
        System.out.println("  - 公交顾客: " + currentSolution.getBusServiceCustomers().size());
        System.out.println("  - 物流顾客: " + currentSolution.getLogisticServiceCustomers().size());
        System.out.println("  - 初始成本: " + initialCost);

        double temperature = this.initialTemperature;
        int iteration = 1;

//        System.out.println("\n优化参数:");
//        System.out.println("  - 初始温度: " + temperature);
//        System.out.println("  - 降温系数: " + coolingRate);
//        System.out.println("  - 终止温度: " + endTemperature);
          System.out.println("  - 最大迭代: " + maxIterations);
        // operatorManager.printOperatorWeights(); // 注释算子权重打印

        while (iteration < maxIterations && temperature > endTemperature) {

            // 选择算子
            RemovalOperator removalOp = operatorManager.selectRemovalOperator();
            InsertionOperator insertionOp = operatorManager.selectInsertionOperator();

            Solution tempSolution = currentSolution.copy();

            List<Customer> removedCustomers = removalOp.remove(tempSolution, 3);
            if (!removedCustomers.isEmpty()) {
                insertionOp.insert(tempSolution, removedCustomers);
            }

            Solution newSolution = tempSolution;

            // 计算修复后的成本
            double newSolutionCost = evaluator.evaluateTotalCost(newSolution);
            newSolution.setTotalCost(newSolutionCost);


            // System.out.println("\n[步骤4: 接受判断]");
            boolean accepted = acceptNewSolution(currentSolution, newSolution, temperature);

            if (accepted) {
                // 注释接受新解打印
                // System.out.println("  -> 接受新解，执行可行化处理");
                newSolution = makeSolutionFeasible(newSolution);
                double finalCost = evaluator.evaluateTotalCost(newSolution);
                newSolution.setTotalCost(finalCost);

                currentSolution = newSolution.copy();

            } else {
                // 注释拒绝新解打印
                // System.out.println("  -> 拒绝新解");
            }

            // 更新最优解
            // System.out.println("\n[步骤5: 更新全局最优解]");
            if (currentSolution.getTotalCost() < bestSolution.getTotalCost()) {
                bestSolution = currentSolution.copy();
                // 注释找到更优解打印
                // System.out.println("  -> 找到更优解! 更新 Best Solution.");
                // System.out.println("  -> 新 Best Solution 成本: " + bestSolution.getTotalCost());
            } else {
                // 注释未找到更优解打印
                // System.out.println("  -> 未找到更优解.");
            }

            // 其他更新
            operatorManager.updateOperatorPerformance(removalOp, insertionOp, newSolution, currentSolution, bestSolution);
            operatorManager.updateAllOperatorWeights();

            iteration++; // 先计数，再判断
            if (iteration % 10 == 0) { // 每10次迭代降温（10、20、30...）
                temperature *= coolingRate;
            }
        }

        // 优化完成
        System.out.println("\n\n=== ALNS优化完成 ===");
        System.out.println("总迭代次数: " + iteration);
        System.out.println("最终 Best Solution 状态:");
        System.out.println("  - 公交顾客: " + bestSolution.getBusServiceCustomers().size());
        System.out.println("  - 物流顾客: " + bestSolution.getLogisticServiceCustomers().size());
        System.out.println("  - 最终最小成本: " + bestSolution.getTotalCost());

        return bestSolution;
    }

    //多时段优化方法
    public Map<Integer, Solution> optimizeMultiple(Map<Integer, Solution> initialSolutions) {
        System.out.println("=== 开始多时段ALNS优化 ===");

        // 1. 深拷贝初始解
        Map<Integer, Solution> currentSolutions = deepCopySolutions(initialSolutions);
        Map<Integer, Solution> bestSolutions = deepCopySolutions(initialSolutions);
        // 2. 初始化独立算子
        TimeWindowIndependentOperator timeWindowIndependentOperator = new TimeWindowIndependentOperator();
        // 硬编码：设置调整客户数量（例如2个）
        timeWindowIndependentOperator.setAdjustmentCount(2);
        // 2. 计算初始总成本
        SolutionEvaluator evaluator = new SolutionEvaluator(parameters);
        double initialTotalCost = calculateGlobalTotalCost(currentSolutions, evaluator);
        System.out.println("初始全局总成本: " + initialTotalCost);

        // 3. 设置初始温度
        double temperature = this.initialTemperature;
        int iteration = 1;

        while (iteration < maxIterations && temperature > endTemperature) {
            // 注释迭代开始打印
            // System.out.println("\n=== 第 " + iteration + " 轮迭代开始 ===");

            // 注释全局状态打印
            // printGlobalStatus(currentSolutions, evaluator, "迭代前");

            double rand = ProbabilityCalculator.nextDouble();
            if (rand < independentProbability) {
//                 使用独立算子
//                 注释算子选择打印
            //     System        .out.println("\n[步骤1: 选择算子]");
                //    System.out.println("  - 独立算子: " + timeWindowIndependentOperator.getName());

                // 深拷贝当前解
                Map<Integer, Solution> tempSolutions = deepCopySolutions(currentSolutions);
                timeWindowIndependentOperator.run(tempSolutions);

                // 计算新解成本
                double newTotalCost = calculateGlobalTotalCost(tempSolutions, evaluator);
                double currentTotalCost = calculateGlobalTotalCost(currentSolutions, evaluator);
                double bestTotalCost = calculateGlobalTotalCost(bestSolutions, evaluator);

                // 接受判断
                boolean accepted = acceptNewSolution(currentTotalCost, newTotalCost, temperature);

                if (accepted) {
                    // 注释接受新解打印
                    // System.out.println("  -> 接受新解，执行可行化处理");
                    // 对每个时段的解进行可行化
                    tempSolutions = makeSolutionsFeasible(tempSolutions);
                    newTotalCost = calculateGlobalTotalCost(tempSolutions, evaluator);

                    currentSolutions = deepCopySolutions(tempSolutions);


                    // 更新独立算子分数
                    updateIndependentOperatorScore(timeWindowIndependentOperator,
                            currentTotalCost, newTotalCost, bestTotalCost);
                } else {
                    // 注释拒绝新解打印
                    // System.out.println("  -> 拒绝新解");
                }

            } else {
                RemovalOperator removalOp = operatorManager.selectRemovalOperator();
                InsertionOperator insertionOp = operatorManager.selectInsertionOperator();

                Map<Integer, Solution> tempSolutions = deepCopySolutions(currentSolutions);
                Map<Integer, List<Customer>> removedCustomersByTimeWindow =
                        removalOp.remove(tempSolutions, 3);

                // 注释移除统计打印
                // int totalRemoved = 0;
                // for (Map.Entry<Integer, List<Customer>> entry : removedCustomersByTimeWindow.entrySet()) {
                //     totalRemoved += entry.getValue().size();
                //     System.out.println("  时段 " + entry.getKey() + " 移除客户: " +
                //             entry.getValue().size() + " 个");
                // }
                // System.out.println("  总计移除: " + totalRemoved + " 个客户");

                // 7. 修复操作（多时段版本）
                // 注释修复操作打印
                // System.out.println("\n[步骤3: 修复操作]");
                int totalRemoved = 0;
                for (List<Customer> list : removedCustomersByTimeWindow.values()) {
                    totalRemoved += list.size();
                }
                if (totalRemoved > 0) {
                    insertionOp.insert(tempSolutions, removedCustomersByTimeWindow);
                }

                // 8. 计算新解成本
                double newTotalCost = calculateGlobalTotalCost(tempSolutions, evaluator);
                double currentTotalCost = calculateGlobalTotalCost(currentSolutions, evaluator);
                double bestTotalCost = calculateGlobalTotalCost(bestSolutions, evaluator);

                // 注释接受判断前状态打印
                // System.out.println("\n[接受判断前状态]");
                // System.out.println("  - 当前全局成本: " + currentTotalCost);
                // System.out.println("  - 新解全局成本: " + newTotalCost);
                // System.out.println("  - 历史最优成本: " + bestTotalCost);

                // 9. 接受判断
                boolean accepted = acceptNewSolution(currentTotalCost, newTotalCost, temperature);

                if (accepted) {
                    // 注释接受新解打印
                    // System.out.println("  -> 接受新解，执行可行化处理");
                    // 对每个时段的解进行可行化
                    tempSolutions = makeSolutionsFeasible(tempSolutions);
                    newTotalCost = calculateGlobalTotalCost(tempSolutions, evaluator);

                    currentSolutions = deepCopySolutions(tempSolutions);

                    // 注释可行化后成本打印
                    // System.out.println("  -> 可行化后最终成本: " + newTotalCost);
                } else {
                    // 注释拒绝新解打印
                    // System.out.println("  -> 拒绝新解");
                }
            }

            // 10. 更新全局最优解
            double updatedCurrentCost = calculateGlobalTotalCost(currentSolutions, evaluator);
            double bestTotalCost = calculateGlobalTotalCost(bestSolutions, evaluator);//1111
            if (updatedCurrentCost < bestTotalCost) {
                bestSolutions = deepCopySolutions(currentSolutions);
                // 注释找到新最优解打印
                // System.out.println("\n 找到新的全局最优解!");
                // System.out.println("  - 新最优成本: " + updatedCurrentCost);
            }

            // 11. 定期更新独立算子权重
            if (iteration % segmentLength == 0) {
                updateIndependentOperatorWeight(timeWindowIndependentOperator);
            }

            // 12. 降温
            iteration++;
            if (iteration % 10 == 0) {
                temperature *= coolingRate;
            }
            // 13. 输出收敛情况
            if ((iteration + 1) % 10 == 0) { // 目前间距是10
                double currentGlobalCost = calculateGlobalTotalCost(currentSolutions, evaluator);
                double bestGlobalCost = calculateGlobalTotalCost(bestSolutions, evaluator);

                // 计算gap：当前解与历史最优解的相对差距
                double ra = 0.0;
                if (initialTotalCost > bestGlobalCost) {
                    // 分母：从初始到最优的改进空间
                    // 分子：当前解离最优还有多少差距
                    ra = (currentGlobalCost/ initialTotalCost ) * 100.0;
                }
                // 输出收敛情况，包含gap
                System.out.printf("迭代 %4d: 当前全局总成本 = %8.2f",
                        iteration + 1, currentGlobalCost, bestGlobalCost, ra);
//                System.out.printf("迭代 %4d: 当前全局总成本 = %8.2f, 历史最优 = %8.2f, Gap = %6.2f%%%n",
//                        iteration + 1, currentGlobalCost, bestGlobalCost, gap);
            }

        }

        // 优化完成
        System.out.println("\n\n=== 多时段ALNS优化完成 ===");
        System.out.println("总迭代次数: " + (iteration-1));
        double finalBestCost = calculateGlobalTotalCost(bestSolutions, evaluator);
        System.out.println("最终全局最优成本: " + finalBestCost);

        return bestSolutions;
    }


    private void updateIndependentOperatorScore(TimeWindowIndependentOperator operator,
                                                double currentCost, double newCost, double bestCost) {
        int score = calculateScoreForIndependentOperator(currentCost, newCost, bestCost);
        operator.updateScore(score);
        operator.incrementUsageCount();
        // 注释算子得分打印
        // System.out.println("  独立算子得分: " + score);
    }

    private int calculateScoreForIndependentOperator(double currentCost, double newCost, double bestCost) {
        if (newCost < bestCost) return 5;    // 找到新的全局最优解
        if (newCost < currentCost) return 3;  // 改善当前解
        if (Math.abs(newCost - currentCost) < 1e-6) return 1; // 接受同等质量的解
        return 0; // 解质量下降
    }

    private void updateIndependentOperatorWeight(TimeWindowIndependentOperator operator) {
        double reactionFactor = 0.3; // 与OperatorManager中的reactionFactor一致
        int score = operator.getScore();
        int usage = Math.max(1, operator.getUsageCount());

        double currentWeight = operator.getWeight();
        double performance = (double) score / usage;
        double newWeight = currentWeight * (1 - reactionFactor) + reactionFactor * performance;

        // 限制权重范围
        newWeight = Math.max(0.1, Math.min(10.0, newWeight));

        operator.updateWeight(newWeight);
        operator.resetScore();
        operator.resetUsageCount();

    }

    private void updateOperatorPerformanceMultiple(RemovalOperator removalOp, InsertionOperator insertionOp,
                                                   double newCost, double currentCost, double bestCost) {
        int removalScore = calculateScoreForIndependentOperator(currentCost, newCost, bestCost);
        int insertionScore = calculateScoreForIndependentOperator(currentCost, newCost, bestCost);

        removalOp.updateScore(removalScore);
        insertionOp.updateScore(insertionScore);
        removalOp.incrementUsageCount();
        insertionOp.incrementUsageCount();

        operatorManager.updateAllOperatorWeights();
    }


private Solution makeSolutionFeasible(Solution solution) {
    // 1. 保存当前的服务模式
    Set<Customer> currentBusCustomers = new HashSet<>(solution.getBusServiceCustomers());
    Set<Customer> currentLogisticCustomers = new HashSet<>(solution.getLogisticServiceCustomers());

    // 2. 执行可行化处理：清理公交路径中的服务站
    solution.clearServiceStations();
    List<Route> cleanedBusRoutes = new ArrayList<>();
    for (Route busRoute : solution.getBusRoutes()) {
        Route cleanedRoute = removeServiceStationsFromRoute(busRoute);
        cleanedBusRoutes.add(cleanedRoute);
    }
    solution.setBusRoutes(cleanedBusRoutes);

    // 3. 重新生成公交车路径（保留原有逻辑）
    Map<Integer, Solution> singlePeriodMap = Collections.singletonMap(solution.getTimeWindow(), solution);
    reoptimizeBusRoutesTool.reoptimizeBusRoutesForALNS(singlePeriodMap, allCustomers);

    // 4. 先恢复服务模式+同步状态（关键：放到物流路径生成前）
    solution.setBusServiceCustomers(currentBusCustomers);
    solution.setLogisticServiceCustomers(currentLogisticCustomers);
    for (Customer customer : currentBusCustomers) {
        customer.setServedByBus(true);
    }
    for (Customer customer : currentLogisticCustomers) {
        customer.setServedByBus(false);
    }

    // 5. 核心：清空旧物流路径，调用新的LGnewRoute（删掉旧的solutionBuilder）
    solution.clearLogisticRoutes(); // 清空旧路径，避免覆盖
    LGnewRoute lgNewRoute = new LGnewRoute();
    lgNewRoute.regenerateAfterALNS(solution, distributionCenter); // 仅调用新逻辑

    return solution;
}

    // 单时段接受判断方法
    private boolean acceptNewSolution(Solution current, Solution newSol, double temperature) {
        double costDiff = newSol.getTotalCost() - current.getTotalCost();

        if (costDiff < 0) {
            return true;
        } else {
            double acceptanceProbability = Math.exp(-costDiff / temperature);
            return Math.random() < acceptanceProbability;
        }
    }

    // 计算全局总成本
    private double calculateGlobalTotalCost(Map<Integer, Solution> solutions, SolutionEvaluator evaluator) {
        double totalCost = 0.0;
        for (Solution solution : solutions.values()) {
            totalCost += evaluator.evaluateTotalCost(solution);
        }
        return totalCost;
    }

    // 多时段接受判断方法
    private boolean acceptNewSolution(double currentCost, double newCost, double temperature) {
        double costDiff = newCost - currentCost;

        if (costDiff < 0) {
            return true;
        } else {
            double acceptanceProbability = Math.exp(-costDiff / temperature);
            return Math.random() < acceptanceProbability;
        }
    }

    // 深拷贝多时段解
    private Map<Integer, Solution> deepCopySolutions(Map<Integer, Solution> solutions) {
        Map<Integer, Solution> copy = new HashMap<>();
        for (Map.Entry<Integer, Solution> entry : solutions.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

private Map<Integer, Solution> makeSolutionsFeasible(Map<Integer, Solution> solutions) {
    Map<Integer, Solution> feasibleSolutions = new HashMap<>();

    for (Map.Entry<Integer, Solution> entry : solutions.entrySet()) {
        int timeWindow = entry.getKey();
        Solution solution = entry.getValue();

        // 1. 保存服务模式
        Set<Customer> currentBusCustomers = new HashSet<>(solution.getBusServiceCustomers());
        Set<Customer> currentLogisticCustomers = new HashSet<>(solution.getLogisticServiceCustomers());

        // 2. 清理公交路径中的服务站
        solution.clearServiceStations();
        List<Route> cleanedBusRoutes = new ArrayList<>();
        for (Route busRoute : solution.getBusRoutes()) {
            Route cleanedRoute = removeServiceStationsFromRoute(busRoute);
            cleanedBusRoutes.add(cleanedRoute);
        }
        solution.setBusRoutes(cleanedBusRoutes);

        // 3. 重新生成公交车路径
        Map<Integer, Solution> singlePeriodMap = Collections.singletonMap(timeWindow, solution);
        reoptimizeBusRoutesTool.reoptimizeBusRoutesForALNS(singlePeriodMap, allCustomers);

        // 4. 恢复服务模式+同步状态
        solution.setBusServiceCustomers(currentBusCustomers);
        solution.setLogisticServiceCustomers(currentLogisticCustomers);
        for (Customer customer : currentBusCustomers) {
            customer.setServedByBus(true);
        }
        for (Customer customer : currentLogisticCustomers) {
            customer.setServedByBus(false);
        }

        // 5. 核心：清空旧路径，调用新的LGnewRoute（删掉旧的solutionBuilder）
        solution.clearLogisticRoutes();
        LGnewRoute lgNewRoute = new LGnewRoute();
        lgNewRoute.regenerateAfterALNS(solution, distributionCenter);

        feasibleSolutions.put(timeWindow, solution);
    }

    return feasibleSolutions;
}

    // 从路径中移除服务站节点
    private Route removeServiceStationsFromRoute(Route originalRoute) {
        Route cleanedRoute = new Route(originalRoute.getVehicle(), originalRoute.getTimeWindow());
        List<Node> originalNodes = originalRoute.getNodes();
        List<Double> originalDemands = originalRoute.getDemands();

        for (int i = 0; i < originalNodes.size(); i++) {
            Node node = originalNodes.get(i);
            double demand = originalDemands.get(i);

            if (!(node instanceof ServiceStation)) {
                cleanedRoute.addNode(node, demand);
            }
        }

        return cleanedRoute;
    }
}