package algorithm.operators;

/**
 * 插入位置信息类
 */
public class InsertionPosition {
    private int index;
    private double costIncrease;

    public InsertionPosition(int index, double costIncrease) {
        this.index = index;
        this.costIncrease = costIncrease;
    }

    // Getter methods
    public int getIndex() { return index; }
    public double getCostIncrease() { return costIncrease; }
}
