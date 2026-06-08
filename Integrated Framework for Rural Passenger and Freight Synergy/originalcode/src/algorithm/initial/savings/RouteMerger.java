package algorithm.initial.savings;

import model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 路径合并器 - 负责路径合并的逻辑
 */
public class RouteMerger {

    public static boolean canMergeRoutes(Route routeI, Route routeJ, Node nodeI, Node nodeJ, double vehicleCapacity) {
        // 检查容量约束
        double totalDemand = routeI.getTotalDemand() + routeJ.getTotalDemand();
        if (totalDemand > vehicleCapacity) {
            return false;
        }

        // 检查节点位置是否允许合并
        List<Node> nodesI = routeI.getNodes();
        List<Node> nodesJ = routeJ.getNodes();

        boolean condition1 = isLastCustomerBeforeReturn(nodesI, nodeI) && isFirstCustomerAfterDepot(nodesJ, nodeJ);
        boolean condition2 = isLastCustomerBeforeReturn(nodesJ, nodeJ) && isFirstCustomerAfterDepot(nodesI, nodeI);

        return condition1 || condition2;
    }

    public static Route mergeRoutes(Route routeI, Route routeJ, Node nodeI, Node nodeJ, int timeWindow, double vehicleCapacity) {
        List<Node> nodesI = routeI.getNodes();
        List<Double> demandsI = routeI.getDemands();
        List<Node> nodesJ = routeJ.getNodes();
        List<Double> demandsJ = routeJ.getDemands();

        List<Node> mergedNodes = new ArrayList<>();
        List<Double> mergedDemands = new ArrayList<>();

        if (isLastCustomerBeforeReturn(nodesI, nodeI) && isFirstCustomerAfterDepot(nodesJ, nodeJ)) {
            // 情况1: routeI + routeJ
            mergedNodes.addAll(nodesI.subList(0, nodesI.size() - 1));
            mergedDemands.addAll(demandsI.subList(0, demandsI.size() - 1));
            mergedNodes.addAll(nodesJ.subList(1, nodesJ.size() - 1));
            mergedDemands.addAll(demandsJ.subList(1, demandsJ.size() - 1));
        } else {
            // 情况2: routeJ + routeI
            mergedNodes.addAll(nodesJ.subList(0, nodesJ.size() - 1));
            mergedDemands.addAll(demandsJ.subList(0, demandsJ.size() - 1));
            mergedNodes.addAll(nodesI.subList(1, nodesI.size() - 1));
            mergedDemands.addAll(demandsI.subList(1, demandsI.size() - 1));
        }

        // 统一在合并路径的末尾添加一个DC
        Node dcNode = nodesI.get(nodesI.size() - 1);
        mergedNodes.add(dcNode);
        mergedDemands.add(0.0);

        validateAndCleanRoute(mergedNodes, mergedDemands);

        // 将清理后的节点和需求添加到新路径中
        Route mergedRoute = new Route(new Vehicle(Vehicle.VehicleType.LOGISTIC, vehicleCapacity), timeWindow);
        for (int i = 0; i < mergedNodes.size(); i++) {
            mergedRoute.addNode(mergedNodes.get(i), mergedDemands.get(i));
        }

        return mergedRoute;
    }


    private static void validateAndCleanRoute(List<Node> nodes, List<Double> demands) {
        if (nodes.isEmpty()) return;

        // 1. 确保路径以DC开始
        if (nodes.get(0).getType() != Node.NodeType.DISTRIBUTION_CENTER) {
            Node dc = findDCNode(nodes);
            if (dc != null) {
                nodes.add(0, dc);
                demands.add(0, 0.0);
                System.out.println("⚠️ 路径不以DC开始，已自动修复。");
            } else {
                System.err.println("❌ 路径中未找到DC节点，无法修复！");
                return;
            }
        }

        // 2. 确保路径以DC结束
        if (nodes.get(nodes.size() - 1).getType() != Node.NodeType.DISTRIBUTION_CENTER) {
            Node dc = findDCNode(nodes);
            if (dc != null) {
                nodes.add(dc);
                demands.add(0.0);
                System.out.println("⚠️ 路径不以DC结束，已自动修复。");
            } else {
                System.err.println("❌ 路径中未找到DC节点，无法修复！");
                return;
            }
        }

        // 3. 清理中间的DC节点
        List<Node> cleanedNodes = new ArrayList<>();
        List<Double> cleanedDemands = new ArrayList<>();
        cleanedNodes.add(nodes.get(0)); // 保留起始DC
        cleanedDemands.add(demands.get(0));

        for (int i = 1; i < nodes.size() - 1; i++) {
            Node node = nodes.get(i);
            if (node.getType() != Node.NodeType.DISTRIBUTION_CENTER) {
                cleanedNodes.add(node);
                cleanedDemands.add(demands.get(i));
            } else {
                System.out.println("ℹ️ 移除中间DC节点: " + node.getId());
            }
        }

        cleanedNodes.add(nodes.get(nodes.size() - 1)); // 保留结束DC
        cleanedDemands.add(demands.get(nodes.size() - 1));

        // 更新节点和需求列表
        nodes.clear();
        nodes.addAll(cleanedNodes);
        demands.clear();
        demands.addAll(cleanedDemands);
    }

    private static Node findDCNode(List<Node> nodes) {
        for (Node node : nodes) {
            if (node.getType() == Node.NodeType.DISTRIBUTION_CENTER) {
                return node;
            }
        }
        return null;
    }


    private static boolean isLastCustomerBeforeReturn(List<Node> nodes, Node targetNode) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node.getId().equals(targetNode.getId())) {
                return i == nodes.size() - 2 && nodes.get(nodes.size() - 1).getType() == Node.NodeType.DISTRIBUTION_CENTER;
            }
        }
        return false;
    }


    private static boolean isFirstCustomerAfterDepot(List<Node> nodes, Node targetNode) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node.getId().equals(targetNode.getId())) {
                return i == 1 && nodes.get(0).getType() == Node.NodeType.DISTRIBUTION_CENTER;
            }
        }
        return false;
    }


    private static double getNodeDemand(Node node) {
        if (node instanceof Customer) {
            return ((Customer) node).getTotalDemand();
        } else if (node instanceof ServiceStation) {
            return ((ServiceStation) node).getReplenishmentDemand();
        }
        return 0;
    }
}