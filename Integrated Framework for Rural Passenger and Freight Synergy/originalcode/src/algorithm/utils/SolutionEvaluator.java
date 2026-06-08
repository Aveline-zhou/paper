

package algorithm.utils;

import model.Solution;
import model.Route;
import model.Customer;
import model.Node;
import model.Vehicle;
import config.Parameters;
import java.util.List;

/**
 * 解评估工具类 - 所有成本计算都从Parameters获取参数
 */
public class SolutionEvaluator {
    private Parameters params;

    public SolutionEvaluator(Parameters params) {
        this.params = params;
    }

    /**
     * 设置参数
     */
    public void setParameters(Parameters params) {
        this.params = params;
    }

    /**
     * 计算解的总成本（移除所有调试输出，仅保留计算逻辑）
     */
    public double evaluateTotalCost(Solution solution) {
        // 1. 公交车服务成本（客户需求×公交服务单价）
        double busServiceCost = calculateBusServiceCost(solution);
        // 2. 物流车运输成本（路径距离×单位距离成本）
        double logisticDistanceCost = calculateLogisticTransportCost(solution);
        // 3. 服务站建设成本
        double stationCost = calculateStationConstructionCost(solution);
        // 4. 库存持有成本
        double inventoryCost = calculateInventoryCost(solution);
        // 总成本汇总
        return busServiceCost + logisticDistanceCost + stationCost + inventoryCost;
    }

    /**
     * 计算公交车服务成本（改为public，供外部调用）
     * 逻辑：公交服务客户的普通需求 × 公交服务单价
     */
    public double calculateBusServiceCost(Solution solution) {
        double busServiceCost = 0.0;
        for (Customer customer : solution.getBusServiceCustomers()) {
            busServiceCost += customer.getNormalDemand() * params.getBusServiceCost();
        }
        return busServiceCost;
    }

    /**
     * 【新增】计算公交车路径运输成本（可选，若需要统计公交路线的距离成本）
     * 逻辑：公交路线的总距离 × 单位距离成本（和物流车一致）
     */
    public double calculateBusRouteTransportCost(Solution solution) {
        double busDistanceCost = 0.0;
        for (Route route : solution.getBusRoutes()) {
            double distance = calculateRouteDistance(route.getNodes());
            busDistanceCost += distance * params.getDistanceCost();
        }
        return busDistanceCost;
    }

    /**
     * 计算物流车运输成本（改为public，供外部调用）
     * 逻辑：物流路线的总距离 × 单位距离成本
     */
    public double calculateLogisticTransportCost(Solution solution) {
        double logisticTransportCost = 0.0;
        for (Route route : solution.getLogisticRoutes()) {
            double distance = calculateRouteDistance(route.getNodes());
            logisticTransportCost += distance * params.getDistanceCost();
        }
        return logisticTransportCost;
    }

    //    /**
//     * 计算服务站建设成本（改为public，可选输出）
//     */
//    public double calculateStationConstructionCost(Solution solution) {
//        return solution.getServiceStations().size() * params.getStationConstructionCost();
//    }
    public double calculateStationConstructionCost(Solution solution) {
        double stationCost = 0.0;
        // 核心修改：按服务站ID获取自定义不确定成本
        for (Node node : solution.getServiceStations()) {
            // 按ID取成本，无需类型转换，兼容所有Node类型
            stationCost += StationCostMapper.getStationCost(node.getId());
        }
        return stationCost;
    }
    /**
     * 计算库存持有成本（改为public，可选输出）
     */
    public double calculateInventoryCost(Solution solution) {
        double inventoryCost = 0.0;
        for (Customer customer : solution.getAllCustomers()) {
            if (customer.getActualDeliveryTime() < customer.getRequiredTimeWindow()) {
                int earlyPeriods = customer.getRequiredTimeWindow() - customer.getActualDeliveryTime();
                inventoryCost += earlyPeriods * params.getInventoryCost() * customer.getTotalDemand();
            }
        }
        return inventoryCost;
    }

    /**
     * 计算单条路径成本（原逻辑不变）
     */
    public double calculateRouteCost(Route route) {
        double cost = 0.0;
        if (route.getVehicle().getType() == Vehicle.VehicleType.BUS) {
            for (Node node : route.getNodes()) {
                if (node instanceof Customer) {
                    Customer customer = (Customer) node;
                    cost += customer.getNormalDemand() * params.getBusServiceCost();
                }
            }
        } else {
            double distance = calculateRouteDistance(route.getNodes());
            cost += distance * params.getDistanceCost();
        }
        return cost;
    }

    /**
     * 计算路径距离的辅助方法（原逻辑不变）
     */
    private double calculateRouteDistance(List<Node> nodes) {
        if (nodes == null || nodes.size() < 2) {
            return 0.0;
        }
        double totalDistance = 0.0;
        for (int i = 0; i < nodes.size() - 1; i++) {
            Node from = nodes.get(i);
            Node to = nodes.get(i + 1);
            totalDistance += calculateDistance(from, to);
        }
        return totalDistance;
    }

    /**
     * 计算两点间距离的辅助方法（原逻辑不变）
     */
    private double calculateDistance(Node from, Node to) {
        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}