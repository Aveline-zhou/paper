package algorithm.initial;

import algorithm.utils.IG;
import config.Parameters;
import model.*;
import java.util.List;
import java.util.Map;

// 初始解专用：仅调用通用IG工具类，简化逻辑
public class ServiceStationInserter {
    private IG ig; // 依赖通用IG工具类

    // 传入候选服务站，初始化IG类
    public ServiceStationInserter(List<Node> candidateServiceStations) {
        this.ig = new IG(candidateServiceStations); // 初始化通用IG工具
    }

    // 优化所有时段公交车路径（初始解专用：仅调用IG，参数为「无需过滤」）
    public void optimizeBusRoutesWithStations(Map<Integer, Solution> solutions, List<Customer> allCustomers) {
        // 遍历所有时段
        for (Solution solution : solutions.values()) {
            // 初始解：无需过滤公交客户、无需过滤节点（核心参数：false, false）
            List<Route> feasibleBusRoutes = ig.processBusRoutes(solution, allCustomers, false, false);
            solution.setBusRoutes(feasibleBusRoutes); // 更新为可行路径
        }
    }

}
