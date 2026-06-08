package algorithm.operators;

import algorithm.initial.CustomerClassifier;
import algorithm.operators.BWR.WorstRemovalOperator;
import algorithm.utils.ProbabilityCalculator;
import model.Node;
import model.Solution;
import model.Customer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 算子管理类
 */
public class OperatorManager {

    private final List<RemovalOperator> removalOperators;
    private final List<InsertionOperator> insertionOperators;

    private final Map<String, Integer> operatorScores;
    private final Map<String, Integer> operatorUsage;

    private final int segmentLength;
    private final double reactionFactor = 0.3;
    private int iterationCount = 0;
    // 新增：跨时段算子需要的参数
    private CustomerClassifier customerClassifier;
    private Map<Integer, List<Node>> timeWindowBusRoutes;

    public OperatorManager(int segmentLength) {//算子统一调用
        this.segmentLength = segmentLength;

        removalOperators = new ArrayList<>();
        insertionOperators = new ArrayList<>();
        removalOperators.add(new RandomRemovalOperator());
        removalOperators.add(new WorstRemovalOperator());
        removalOperators.add(new DistanceBasedRemovalOperator());
        insertionOperators.add(new RegretInsertionOperator());
        insertionOperators.add(new GreedyInsertionOperator());

        // 初始化表现记录
        operatorScores = new HashMap<>();
        operatorUsage = new HashMap<>();
        for (RemovalOperator op : removalOperators) {
            operatorScores.put(op.getName(), 0);
            operatorUsage.put(op.getName(), 0);
        }
        for (InsertionOperator op : insertionOperators) {
            operatorScores.put(op.getName(), 0);
            operatorUsage.put(op.getName(), 0);
        }
    }

    // 选择移除算子
    public RemovalOperator selectRemovalOperator() {
        double[] weights = extractWeights(removalOperators);
        int selectedIndex = ProbabilityCalculator.selectByWeight(weights);

        RemovalOperator selected = removalOperators.get(selectedIndex);

        // 关键修复：如果选择了WorstRemoval且它权重很低，强制使用RandomRemoval
        if (selected instanceof WorstRemovalOperator) {
            double worstRemovalWeight = selected.getWeight();
            if (worstRemovalWeight < 0.5) {
                for (RemovalOperator op : removalOperators) {
                    if (op instanceof RandomRemovalOperator) {
                        return op;
                    }
                }
            }
        }
        return selected;
    }

    // 选择插入算子
    public InsertionOperator selectInsertionOperator() {
        double[] weights = extractWeights(insertionOperators);
        int selectedIndex = ProbabilityCalculator.selectByWeight(weights);
        return insertionOperators.get(selectedIndex);
    }

    // 更新算子表现
    public void updateOperatorPerformance(RemovalOperator removalOp, InsertionOperator insertionOp,
                                          Solution newSolution, Solution currentSolution, Solution bestSolution) {
        // 检查移除算子是否实际移除了客户点
        boolean removalEffective = isRemovalEffective(newSolution, currentSolution);

        int removalScore = calculateOperatorScore(newSolution, currentSolution, bestSolution, removalEffective);
        int insertionScore = calculateOperatorScore(newSolution, currentSolution, bestSolution, true);

        // 更新算子自身的分数
        removalOp.updateScore(removalScore);
        insertionOp.updateScore(insertionScore);

        // 同步更新管理类的表现记录
        String removalName = removalOp.getName();
        String insertionName = insertionOp.getName();

        operatorScores.put(removalName, operatorScores.get(removalName) + removalScore);
        operatorScores.put(insertionName, operatorScores.get(insertionName) + insertionScore);
        operatorUsage.put(removalName, operatorUsage.get(removalName) + 1);
        operatorUsage.put(insertionName, operatorUsage.get(insertionName) + 1);

        // 同步更新算子自身的使用次数
        removalOp.incrementUsageCount();
        insertionOp.incrementUsageCount();

        System.out.println("  算子表现更新: " + removalName + "=" + removalScore +
                ", " + insertionName + "=" + insertionScore);
    }

    // 检查移除算子是否有效
    private boolean isRemovalEffective(Solution newSolution, Solution currentSolution) {
        int currentBus = currentSolution.getBusServiceCustomers().size();
        int currentLogistic = currentSolution.getLogisticServiceCustomers().size();
        int newBus = newSolution.getBusServiceCustomers().size();
        int newLogistic = newSolution.getLogisticServiceCustomers().size();

        return (currentBus != newBus) || (currentLogistic != newLogistic);
    }

    // 批量更新权重
    public void updateAllOperatorWeights() {
        iterationCount++;

        // 每segmentLength次迭代更新一次权重
        if (iterationCount % segmentLength == 0) {
            System.out.println("=== 更新算子权重（第" + iterationCount + "次迭代）===");

            // 更新移除算子权重
            for (RemovalOperator op : removalOperators) {
                updateSingleOperatorWeight(op);
            }

            // 更新插入算子权重
            for (InsertionOperator op : insertionOperators) {
                updateSingleOperatorWeight(op);
            }

            // 打印更新后的权重
            printOperatorWeights();
        }
    }

    // 统一更新单个算子的权重
    private void updateSingleOperatorWeight(BaseOperator op) {
        String opName = op.getName();
        int score = operatorScores.get(opName);
        int usage = Math.max(1, operatorUsage.get(opName));

        double currentWeight = op.getWeight();
        double performance = (double) score / usage;
        double newWeight = currentWeight * (1 - reactionFactor) + reactionFactor * performance;

        // 限制权重范围
        newWeight = Math.max(0.1, Math.min(10.0, newWeight));

        op.updateWeight(newWeight);

        // 关键修改：只重置算子分数，不重置使用次数（因为BaseOperator可能没有resetUsageCount方法）
        op.resetScore();

        // 重置管理类记录
        operatorScores.put(opName, 0);
        operatorUsage.put(opName, 0);

//        System.out.println("  " + opName + ": 分数=" + score + ", 使用=" + usage +
//                ", 性能=" + String.format("%.2f", performance) +
//                ", 新权重=" + String.format("%.2f", newWeight));
    }

    // 计算算子分数
    private int calculateOperatorScore(Solution newSolution, Solution currentSolution,
                                       Solution bestSolution, boolean operatorEffective) {
        // 如果算子没有产生效果，给最低分
        if (!operatorEffective) {
            return 0;
        }

        double newCost = newSolution.getTotalCost();
        double currentCost = currentSolution.getTotalCost();
        double bestCost = bestSolution.getTotalCost();

        if (newCost < bestCost) return 5;    // 找到新的全局最优解
        if (newCost < currentCost) return 3;  // 改善当前解
        if (Math.abs(newCost - currentCost) < 1e-6) return 1; // 接受同等质量的解
        return 0; // 解质量下降
    }

    // 提取权重数组
    private double[] extractWeights(List<? extends BaseOperator> operators) {
        double[] weights = new double[operators.size()];
        for (int i = 0; i < operators.size(); i++) {
            weights[i] = operators.get(i).getWeight();
        }
        return weights;
    }

    // 打印权重
    public void printOperatorWeights() {
        System.out.print("移除算子权重: ");
        for (RemovalOperator op : removalOperators) {
            System.out.print(op.getName() + "=" + String.format("%.2f", op.getWeight()) + " ");
        }
        System.out.print(" | 插入算子权重: ");
        for (InsertionOperator op : insertionOperators) {
            System.out.print(op.getName() + "=" + String.format("%.2f", op.getWeight()) + " ");
        }
        System.out.println();
    }

    // 获取特定移除算子的权重
    public double getRemovalWeight(String operatorName) {
        for (RemovalOperator op : removalOperators) {
            if (op.getName().equals(operatorName)) {
                return op.getWeight();
            }
        }
        return 0.0;
    }

    public int getSegmentLength() {
        return segmentLength;
    }

    public List<RemovalOperator> getRemovalOperators() {
        return removalOperators;
    }

    public List<InsertionOperator> getInsertionOperators() {
        return insertionOperators;
    }
}