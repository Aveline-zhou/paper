package algorithm.initial.savings;

import model.Node;
import algorithm.utils.DistanceCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * 节约值计算器 - 负责计算所有节点对的节约值
 */
public class SavingsCalculator {

    /**
     * 计算所有节点对的节约值
     */
    public static List<Savings> calculateAllSavings(List<Node> serviceNodes, Node distributionCenter) {
        List<Savings> savingsList = new ArrayList<>();

        for (int i = 0; i < serviceNodes.size(); i++) {
            for (int j = i + 1; j < serviceNodes.size(); j++) {
                Node nodeI = serviceNodes.get(i);
                Node nodeJ = serviceNodes.get(j);

                double savingValue = calculateSavingValue(nodeI, nodeJ, distributionCenter);
                savingsList.add(new Savings(nodeI, nodeJ, savingValue));
            }
        }

        return savingsList;
    }

    /**
     * 计算单个节点对的节约值
     */
    private static double calculateSavingValue(Node nodeI, Node nodeJ, Node distributionCenter) {
        double distanceDCtoI = DistanceCalculator.calculateEuclideanDistance(distributionCenter, nodeI);
        double distanceDCtoJ = DistanceCalculator.calculateEuclideanDistance(distributionCenter, nodeJ);
        double distanceItoJ = DistanceCalculator.calculateEuclideanDistance(nodeI, nodeJ);

        return distanceDCtoI + distanceDCtoJ - distanceItoJ;
    }
}