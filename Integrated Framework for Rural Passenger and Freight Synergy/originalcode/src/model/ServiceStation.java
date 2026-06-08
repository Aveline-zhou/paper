package model;

/**
 * 服务站类，包含补货需求信息（新增IG算法生成标识）
 */
public class ServiceStation extends Node {
    private double replenishmentDemand; // 补货需求
    private boolean isIGGenerated;      // 新增：是否为IG算法插入的服务站（默认true）

    // 原有构造函数保留，新增IG标识默认值
    public ServiceStation(String id, double x, double y, double replenishmentDemand) {
        super(id, x, y, NodeType.SERVICE_STATION);
        this.replenishmentDemand = replenishmentDemand;
        this.isIGGenerated = true; // IG插站默认标记
    }

    // 预留非IG场景
    public ServiceStation(String id, double x, double y, double replenishmentDemand, boolean isIGGenerated) {
        super(id, x, y, NodeType.SERVICE_STATION);
        this.replenishmentDemand = replenishmentDemand;
        this.isIGGenerated = isIGGenerated;
    }

    // 新增IG标识的Getter/Setter
    public boolean isIGGenerated() {
        return isIGGenerated;
    }

    public void setIGGenerated(boolean IGGenerated) {
        isIGGenerated = IGGenerated;
    }

    public double getReplenishmentDemand() {
        return replenishmentDemand;
    }

    public void setReplenishmentDemand(double replenishmentDemand) {
        this.replenishmentDemand = replenishmentDemand;
    }

    // 新增IG标识到toString
    @Override
    public String toString() {
        return super.toString() + "[补货需求=" + replenishmentDemand + ", IG生成=" + isIGGenerated + "]";
    }
}