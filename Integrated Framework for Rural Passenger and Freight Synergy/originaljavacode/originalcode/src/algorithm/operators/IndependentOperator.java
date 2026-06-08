package algorithm.operators;

import java.util.Map;
import model.Solution;

public interface IndependentOperator extends BaseOperator {
    /**
     * 运行独立算子（多时段版本）
     * @param allSolutions 所有时段的解决方案
     */
    void run(Map<Integer, Solution> allSolutions);
}