package algorithm.initial;

import config.Parameters;
import model.Customer;
import model.Node;
import model.Route;
import model.Solution;
import model.Vehicle;
import java.util.List;

import static algorithm.utils.IDrange.FIRST_CUSTOMER_ID;
import static algorithm.utils.IDrange.LAST_CUSTOMER_ID;


public class BusRouteGenerator {
    private Parameters parameters;
    public BusRouteGenerator() {
        this.parameters = Parameters.getInstance();
    }

    public void generateInitialBusRoutes(Solution solution, List<Node> busRoute) {
        // 空路径直接返回
        if (busRoute == null || busRoute.isEmpty()) {return;}
        Vehicle bus = new Vehicle(Vehicle.VehicleType.BUS, parameters.getBusCapacity());
        Route route = new Route(bus, solution.getTimeWindow());

        // 遍历固定路径节点
        for (Node node : busRoute) {
            double demand = 0.0;

            try {
                int nodeId = Integer.parseInt(node.getId());
                if (nodeId >= FIRST_CUSTOMER_ID && nodeId <= LAST_CUSTOMER_ID) {
                    // 查找对应的客户点
                    for (Customer customer : solution.getBusServiceCustomers()) {
                        String baseId = customer.getId().split("T")[0];
                        if (String.valueOf(nodeId).equals(baseId)) {
                            demand = customer.getNormalDemand();
                            break;
                        }
                    }
                }
            } catch (NumberFormatException e) {// 不是数字ID，跳过
            }
            route.addNode(node, demand);
        }
        solution.addBusRoute(route);}}