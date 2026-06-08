package algorithm.operators;

import model.Solution;
import model.Customer;
import java.util.List;
import java.util.Map;

public interface RemovalOperator extends BaseOperator {
    // 单时段版本
    List<Customer> remove(Solution solution, int nq);

    // 多时段版本（返回类型改为 Map<Integer, List<Customer>>）
    Map<Integer, List<Customer>> remove(Map<Integer, Solution> allSolutions, int nq);

    // 注意：不需要重复声明 BaseOperator 中的方法
    // incrementUsageCount() 和 resetUsageCount() 已经在 BaseOperator 中定义了
}