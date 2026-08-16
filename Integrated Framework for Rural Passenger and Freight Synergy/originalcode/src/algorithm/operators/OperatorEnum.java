package algorithm.operators;

/**
 * 算子枚举（序列标明：ordinal() 对应原列表索引，0开始）
 */
public enum OperatorEnum {
    // 移除算子（序列0、1，与原initializeOperators顺序一致）
    RANDOM_REMOVAL("RandomRemoval", OperatorType.REMOVAL, 0),
    WORST_REMOVAL("WorstRemoval", OperatorType.REMOVAL, 1),

    // 插入算子（序列0、1，与原initializeOperators顺序一致）
    GREEDY_INSERTION("GreedyInsertion", OperatorType.INSERTION, 0),
    TIME_WINDOW_ADJUSTMENT("TimeWindowAdjustment", OperatorType.INSERTION, 1);

    private final String operatorName; // 与算子getName()一致
    private final OperatorType operatorType; // 算子类型（移除/插入）
    private final int sequence; // 序列（与原列表索引一致）

    OperatorEnum(String operatorName, OperatorType operatorType, int sequence) {
        this.operatorName = operatorName;
        this.operatorType = operatorType;
        this.sequence = sequence;
    }

    // 根据算子名称和类型获取枚举（用于ALNS中匹配算子）
    public static OperatorEnum getByTypeAndName(OperatorType type, String name) {
        for (OperatorEnum e : values()) {
            if (e.operatorType == type && e.operatorName.equals(name)) {
                return e;
            }
        }
        throw new IllegalArgumentException("无匹配算子：类型=" + type + ", 名称=" + name);
    }

    // 算子类型（内部枚举，区分移除/插入）
    public enum OperatorType {
        REMOVAL, INSERTION
    }

    // getter（保持原有变量名风格）
    public String getOperatorName() {
        return operatorName;
    }

    public OperatorType getOperatorType() {
        return operatorType;
    }

    public int getSequence() {
        return sequence;
    }
}