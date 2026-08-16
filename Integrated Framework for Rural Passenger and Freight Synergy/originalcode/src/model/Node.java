package model;
/**
 * 节点基类，包含位置信息和类型
 */
public class Node {
    private String id;
    private NodeType type;
    private double x;
    private double y;

    public enum NodeType {
        DISTRIBUTION_CENTER, // 配送中心
        BUS_STOP,           // 公交车站点
        CUSTOMER,           // 顾客节点
        SERVICE_STATION     // 服务站
    }

    public Node(String id, double x, double y, NodeType type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    // Getter and Setter methods
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + "(" + type + ")";
    }
}