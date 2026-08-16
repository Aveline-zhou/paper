package algorithm.operators;

import model.Solution;
import model.Customer;
import java.util.List;
import java.util.Map;

public interface InsertionOperator extends BaseOperator {
    // 单时段版本
    void insert(Solution solution, List<Customer> customers);

    // 多时段版本（新增）
    void insert(Map<Integer, Solution> allSolutions,
                Map<Integer, List<Customer>> timeWindowToRemovedCustomers);

}