package algorithm.utils;
import model.Node;

/**
 * 距离计算工具类
 */
public class DistanceCalculator {

    /**
     * 计算两个节点之间的欧氏距离
     */
    public static double calculateEuclideanDistance(Node node1, Node node2) {
        double deltaX = node1.getX() - node2.getX();
        double deltaY = node1.getY() - node2.getY();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    /**
     * 计算路径总距离
     */
    public static double calculateRouteDistance(java.util.List<Node> nodes) {
        if (nodes == null || nodes.size() < 2) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 0; i < nodes.size() - 1; i++) {
            totalDistance += calculateEuclideanDistance(nodes.get(i), nodes.get(i + 1));
        }
        return totalDistance;
    }

    /**
     * 计算插入节点后的距离增量
     */
    public static double calculateInsertionCost(Node prev, Node newNode, Node next) {
        double originalDistance = calculateEuclideanDistance(prev, next);
        double newDistance = calculateEuclideanDistance(prev, newNode) +
                calculateEuclideanDistance(newNode, next);
        return newDistance - originalDistance;
    }
}