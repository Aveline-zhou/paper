package model;

import java.util.Objects;

/**
 * 顾客点类，包含货物需求和配送时段信息
 */
public class Customer extends Node {
    private double normalDemand;
    private double specialDemand;
    private int requiredTimeWindow;
    private boolean servedByBus;
    private int actualDeliveryTime;

    public Customer(String id, double x, double y, double normalDemand,
                    double specialDemand, int timeWindow) {
        super(id, x, y, NodeType.CUSTOMER);
        this.normalDemand = normalDemand;
        this.specialDemand = specialDemand;
        this.requiredTimeWindow = timeWindow;
        this.actualDeliveryTime = timeWindow; // 默认与实际需求时段相同
        this.servedByBus = false;
    }



    public double getNormalDemand() {
        return normalDemand;
    }

    public void setNormalDemand(double normalDemand) {
        this.normalDemand = normalDemand;
    }

    public double getSpecialDemand() {
        return specialDemand;
    }

    public void setSpecialDemand(double specialDemand) {
        this.specialDemand = specialDemand;
    }

    public int getRequiredTimeWindow() {
        return requiredTimeWindow;
    }

    public void setRequiredTimeWindow(int requiredTimeWindow) {
        this.requiredTimeWindow = requiredTimeWindow;
    }

    public boolean isServedByBus() {
        return servedByBus;
    }

    public void setServedByBus(boolean servedByBus) {
        this.servedByBus = servedByBus;
    }

    public int getActualDeliveryTime() {
        return actualDeliveryTime;
    }

    public void setActualDeliveryTime(int actualDeliveryTime) {
        this.actualDeliveryTime = actualDeliveryTime;
    }

    public double getTotalDemand() {
        return normalDemand + specialDemand; // 总需求=普通需求+特殊需求
    }


    /**
     * 检查是否可以提前配送
     */
    public boolean canDeliverEarlier() {
        return actualDeliveryTime > 1; // 至少可以提前到第一个时段
    }

    /**
     * 将配送时间提前一个时段
     */
    public boolean deliverEarlier() {
        if (canDeliverEarlier()) {
            actualDeliveryTime--;
            return true;
        }
        return false;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        // 同时比较ID和时段，确保不同时段的同一节点被视为不同对象
        return Objects.equals(this.getId(), customer.getId()) &&
                this.requiredTimeWindow == customer.requiredTimeWindow;
    }
    @Override
    public int hashCode() {
        // 同时基于ID和时段生成hashCode
        return Objects.hash(this.getId(), requiredTimeWindow);
    }
    @Override
    public String toString() {
        return String.format("Customer{id=%s, displayName=%s, x=%.1f, y=%.1f, normal=%.1f, special=%.1f, window=%d}",
                getId(), getX(), getY(), normalDemand, specialDemand, requiredTimeWindow);
    }

}