package algorithm.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;


public class ProbabilityCalculator {
    private static final Random random = new Random();

    private ProbabilityCalculator() {}


    public static double[] computeCumulativeProbabilities(double[] weights) {
        // 1. 计算所有权重之和
        double sumAllWeights = 0;
        for (double weight : weights) {
            sumAllWeights += weight;
        }

        // 2. 计算累积权重
        double[] cumulativeWeights = new double[weights.length];
        double sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i];
            cumulativeWeights[i] = sum;
        }

        // 3. 计算累积概率
        double[] cumulativeProbabilities = new double[weights.length];
        for (int i = 0; i < cumulativeProbabilities.length; i++) {
            BigDecimal a = new BigDecimal(Double.toString(cumulativeWeights[i]));
            BigDecimal b = new BigDecimal(Double.toString(sumAllWeights));
            cumulativeProbabilities[i] = a.divide(b, 10, RoundingMode.HALF_UP).doubleValue();
        }
        // 确保最后一个累积概率为1.0
        cumulativeProbabilities[cumulativeProbabilities.length - 1] = 1.0;
        return cumulativeProbabilities;
    }


    public static int rouletteWheelSelection(double[] cumulativeProbabilities) {
        double randomValue = random.nextDouble();// Random.nextDouble() 返回 [0.0, 1.0) 的伪随机double值
        if (randomValue == 0.0) {return 0;}//随机数为0返回第一个索引
        for (int i = 0; i < cumulativeProbabilities.length; i++) {
            if (randomValue <= cumulativeProbabilities[i]) {
                return i;
            }
        }
        // 如果由于浮点精度问题没有返回，返回最后一个索引
        return cumulativeProbabilities.length - 1;
    }

    public static int selectByWeight(double[] weights) {
        double[] cumulativeProbabilities = computeCumulativeProbabilities(weights);
        return rouletteWheelSelection(cumulativeProbabilities);
    }

    public static double calculateAcceptanceProbability(double currentCost, double newCost, double temperature) {
        if (newCost < currentCost) {
            return 1.0;
        }
        return Math.exp((currentCost - newCost) / temperature);
    }
    // 添加公共静态方法获取随机数
    public static double nextDouble() {
        return random.nextDouble();
    }
}