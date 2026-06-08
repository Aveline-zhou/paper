package algorithm.initial.savings;

import model.Node;

/**
 * 节约值数据类 - 存储节点对和对应的节约值
 */
public class Savings implements Comparable<Savings> {
    private final Node nodeI;
    private final Node nodeJ;
    private final double value;

    public Savings(Node nodeI, Node nodeJ, double value) {
        this.nodeI = nodeI;
        this.nodeJ = nodeJ;
        this.value = value;
    }

    public Node getNodeI() {
        return nodeI;
    }

    public Node getNodeJ() {
        return nodeJ;
    }

    public double getValue() {
        return value;
    }

    @Override
    public int compareTo(Savings other) {
        return Double.compare(other.value, this.value); // 降序排序
    }

    @Override
    public String toString() {
        return String.format("Savings{%s-%s: %.2f}", nodeI.getId(), nodeJ.getId(), value);
    }
}