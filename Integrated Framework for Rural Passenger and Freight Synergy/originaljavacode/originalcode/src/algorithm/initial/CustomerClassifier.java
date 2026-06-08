package algorithm.initial;

import model.Customer;
import model.Node;
import model.Solution;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import static algorithm.utils.IDrange.FIRST_CUSTOMER_ID;
import static algorithm.utils.IDrange.LAST_CUSTOMER_ID;

public class CustomerClassifier {

    //公交路线包含的客户点分配给公交车，其他给物流车
    public void classifyCustomers(Solution solution, List<Customer> timeWindowCustomers, List<Node> busRoute) {
        // 提取公交路线中的客户ID集合（复用公共逻辑）
        Set<Integer> busCustomerIds = getBusCustomerIds(busRoute);
        // 遍历客户分类
        for (Customer customer : timeWindowCustomers) {
            int customerNumberId = extractCustomerNumberId(customer.getId());
            if (customer.getNormalDemand() > 0 && customer.getSpecialDemand() == 0 && busCustomerIds.contains(customerNumberId)) {
                solution.addBusCustomer(customer); // 正常需求>0 且 特殊需求=0 且 公交路线包含该客户
            } else {
                solution.addLogisticCustomer(customer);
            }
        }
    }

    //客户是否在公交路线上保留外部调用
    public boolean isOnBusRoute(Customer customer, List<Node> busRoute) {
        int customerNumberId = extractCustomerNumberId(customer.getId());
        // 复用公共逻辑：获取公交路线客户ID集合后直接判断
        return getBusCustomerIds(busRoute).contains(customerNumberId);
    }

    //提取公交路线中包含的客户点ID
    private Set<Integer> getBusCustomerIds(List<Node> busRoute) {
        Set<Integer> ids = new HashSet<>();
        for (Node node : busRoute) {
            try {
                int nodeId = Integer.parseInt(node.getId());
                if (nodeId >= FIRST_CUSTOMER_ID && nodeId <= LAST_CUSTOMER_ID) {
                    ids.add(nodeId);
                }
            } catch (NumberFormatException ignored) {} // 忽略非数字ID
        }
        return ids;
    }

    private int extractCustomerNumberId(String customerId) {
        try {
            return Integer.parseInt(customerId.split("T")[0]);
        } catch (Exception e) {
            return -1; // 无效ID
        }
    }
}
