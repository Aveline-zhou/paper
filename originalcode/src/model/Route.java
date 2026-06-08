package model;

import algorithm.utils.SolutionEvaluator;
import config.Parameters;
import java.util.*;
//车辆路径，管理路径节点、需求和成本计算
public class Route {
    private String id;
    private Vehicle vehicle;
    private List<Node> nodes;
    private List<Double> demands; // 存储每个节点的需求
    private double totalCost;
    private double totalDemand;
    private int timeWindow;
    private SolutionEvaluator evaluator;

    public Route(Vehicle vehicle, int timeWindow) {
        this.vehicle = vehicle;
        this.timeWindow = timeWindow;
        this.nodes = new ArrayList<>();
        this.demands = new ArrayList<>();
        this.totalCost = 0.0;
        this.totalDemand = 0.0;
        this.id = "Routetasktime_" + timeWindow + "_" + vehicle.getId();
        this.evaluator = new SolutionEvaluator(Parameters.getInstance());
    }

    //添加节点（初始路径生成用，允许超载，后续通过服务站插入处理可行性）
    public boolean addNode(Node node, double demand) {
        nodes.add(node);// 添加节点到序列末尾
        demands.add(demand);// 添加对应需求
        if (node instanceof Customer) {totalDemand += demand;} //客户需求计入总需求
        recalculateCost();
        return true;
    }
    //插入节点（路径调整用，包含容量检查）
    public boolean insertNode(int index, Node node, double demand) {
        if (index < 0 || index > nodes.size()) {
            return false;
        }
        if (totalDemand + demand > vehicle.getCapacity()) {return false;}
        nodes.add(index, node);// 在指定位置插入节点
        demands.add(index, demand);// 插入对应需求
        totalDemand += demand;// 更新总需求
        recalculateCost();
        return true;
    }

    //移除节点并同步更新需求和成本
    public boolean removeNode(Node node) {
        int index = nodes.indexOf(node);// 查找节点位置
        if (index != -1) {
            double demand = demands.get(index);
            nodes.remove(index);
            demands.remove(index);
            totalDemand -= demand;
            recalculateCost();
            return true;
        }
        return false;
    }
    //获取所有节点的需求列表
    public List<Double> getDemands() {
        return new ArrayList<>(demands);
    }
    //计算路径总需求
    public double calculateTotalDemand() {
        return totalDemand;
    }
    //获取指定节点的需求
    public double getNodeDemand(Node node) {
        int index = nodes.indexOf(node);
        return index != -1 ? demands.get(index) : 0.0;
    }
    //重新计算路径成本
    private void recalculateCost() {
        this.totalCost = evaluator.calculateRouteCost(this);
    }

    //设置SolutionEvaluator
    public void setEvaluator(SolutionEvaluator evaluator) {
        this.evaluator = evaluator;
        recalculateCost();
    }
    //设置节点列表，自动根据车辆类型计算需求
    public void setNodes(List<Node> nodes) {
        this.nodes = new ArrayList<>(nodes);
        this.demands = new ArrayList<>();
        this.totalDemand = 0.0;

        for (Node node : nodes) {
            double nodeDemand = 0.0;

            if (node instanceof Customer) {
                Customer customer = (Customer) node;
                if (this.vehicle.getType() == Vehicle.VehicleType.BUS) {
                    nodeDemand = customer.getNormalDemand();
                } else {
                    nodeDemand = customer.getTotalDemand();
                }
                totalDemand += nodeDemand;
            }
            else if (node instanceof ServiceStation) {
                ServiceStation station = (ServiceStation) node;
                nodeDemand = station.getReplenishmentDemand();

                // ✅ 区分车辆类型
                if (this.vehicle.getType() == Vehicle.VehicleType.LOGISTIC) {
                    totalDemand += nodeDemand;  // 物流车计入服务站需求
                }
                // 公交车不计入
            }

            demands.add(nodeDemand);
        }
        recalculateCost();
    }
//    public void setNodes(List<Node> nodes) {
//        this.nodes = new ArrayList<>(nodes);
//        this.demands = new ArrayList<>();
//        this.totalDemand = 0.0;
//
//        for (Node node : nodes) {
//            double nodeDemand = 0.0;
//            if (node instanceof Customer) {
//                Customer customer = (Customer) node;
//                if (this.vehicle.getType() == Vehicle.VehicleType.BUS) {
//                    nodeDemand = customer.getNormalDemand();
//                } else {
//                    nodeDemand = customer.getTotalDemand();
//                }
//                totalDemand += nodeDemand;  // 只有客户需求才累加
//            } else if (node instanceof ServiceStation) {
//                ServiceStation station = (ServiceStation) node;// ServiceStation需求处理
//                nodeDemand = station.getReplenishmentDemand();
//            }
//            // 其他节点类型为0
//            demands.add(nodeDemand);
//        }
//        recalculateCost();
//    }
    public void setNodesWithDemands(List<Node> nodes, List<Double> demands) {
        if (nodes.size() != demands.size()) {
            throw new IllegalArgumentException("！！！");
        }
        this.nodes = new ArrayList<>(nodes);
        this.demands = new ArrayList<>(demands);

        // ✅ 根据车辆类型计算总需求
        this.totalDemand = 0.0;
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            if (node instanceof Customer) {
                // 客户需求总是计入
                this.totalDemand += demands.get(i);
            }
            else if (node instanceof ServiceStation) {
                // 服务站需求：只有物流车才计入
                if (this.vehicle.getType() == Vehicle.VehicleType.LOGISTIC) {
                    this.totalDemand += demands.get(i);
                }
                // 公交车不计入服务站需求
            }
            // 其他节点（如配送中心、公交站点）需求为0
        }

        recalculateCost();
    }

    // Getter and Setter 方法
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public List<Node> getNodes() { return new ArrayList<>(nodes); }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    public double getTotalDemand() {
        // 完全对齐SolutionEvaluator的成本计算逻辑：仅累加路径内客户的normalDemand（BUS类型）
        double realTotal = 0.0;
        for (Node node : nodes) {
            if (node instanceof Customer) {
                Customer customer = (Customer) node;
                // 核心：直接读取客户的normalDemand（和成本计算的数据源完全一致）
                if (this.vehicle.getType() == Vehicle.VehicleType.BUS) {
                    realTotal += customer.getNormalDemand();
                } else {
                    // 物流车逻辑保留，不影响公交计算
                    realTotal += customer.getTotalDemand();
                }
            }
        }
        return realTotal;
    }
//    public double getTotalDemand() {
//        // 重新计算，只包含客户需求
//        double realTotal = 0.0;
//        for (int i = 0; i < nodes.size(); i++) {Node node = nodes.get(i);
//            if (node instanceof Customer) {
//                realTotal += demands.get(i);}}
//        return realTotal;}
    public void setTotalDemand(double totalDemand) { this.totalDemand = totalDemand; }

    public int getTimeWindow() { return timeWindow; }
    public void setTimeWindow(int timeWindow) { this.timeWindow = timeWindow; }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public int size() {
        return nodes.size();
    }

    @Override
    public String toString() {
        return "Route{" + id + ", nodes=" + nodes + ", demands=" + demands + ", cost=" + totalCost + "}";
    }
    // ========== 1. 核心：获取客户在路径中的索引（基于ID匹配，解决对象引用问题） ==========
    /**
     * 根据客户ID查找客户在路径中的索引（而非对象引用）
     * @param customer 目标客户
     * @return 索引（不存在返回-1）
     */
//    public int getCustomerIndex(Customer customer) {
//        List<Node> nodes = getNodes();
//        for (int i = 0; i < nodes.size(); i++) {
//            Node node = nodes.get(i);
//            // 关键：基于ID匹配，而非对象引用（解决不同实例但同ID的匹配问题）
//            if (node instanceof Customer && ((Customer) node).getId() == customer.getId()) {
//                return i;
//            }
//        }
//        return -1;
//    }
    public int getCustomerIndex(Customer customer) {
        List<Node> nodes = getNodes();
        if (customer == null || customer.getId() == null) {
            return -1;
        }
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node instanceof Customer) {
                Customer c = (Customer) node;
                // 关键修复：String类型ID用equals比较，处理null
                if (c.getId() != null && c.getId().equals(customer.getId())) {
                    return i;
                }
            }
        }
        return -1;
    }
// ========== 2. 核心：获取指定索引的前驱节点（为“原位置后插入”提供锚点） ==========
    /**
     * 获取指定索引的前驱节点（索引≤0或≥节点总数时返回null）
     * @param index 目标索引
     * @return 前驱节点（null表示无）
     */
    public Node getPreviousNode(int index) {
        List<Node> nodes = getNodes();
        if (index <= 0 || index >= nodes.size()) {
            return null;
        }
        return nodes.get(index - 1);
    }

// ========== 3. 核心：在路径中找到与目标节点匹配的索引（基于类型+ID，支持前驱节点定位） ==========
    /**
     * 根据节点类型+ID查找节点在路径中的索引（解决不同实例但同ID的匹配问题）
     * @param targetNode 目标节点（Customer/ServiceStation）
     * @return 索引（不存在返回-1）
     */
    public int findNodeIndex(Node targetNode) {
        List<Node> nodes = getNodes();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            // 第一步：类型必须完全一致（Customer不能匹配ServiceStation）
            if (node.getClass() != targetNode.getClass()) {
                continue;
            }
            // 第二步：基于ID匹配（Customer按id，ServiceStation按id）
            if (node instanceof Customer) {
                Customer c1 = (Customer) node;
                Customer c2 = (Customer) targetNode;
                if (c1.getId() == c2.getId()) {
                    return i;
                }
            } else if (node instanceof ServiceStation) {
                ServiceStation s1 = (ServiceStation) node;
                ServiceStation s2 = (ServiceStation) targetNode;
                // 注意：ServiceStation的id若为String类型，用equals匹配；若为int，用==
                if (s1.getId().equals(s2.getId())) {
                    return i;
                }
            }
            // 其他节点类型（如配送中心）暂不支持匹配，返回-1
        }
        return -1;
    }
}