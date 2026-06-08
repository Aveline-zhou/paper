package model;

/**
 * 车辆类（已支持ID设置）
 */
public class Vehicle {
    private String id;
    private VehicleType type;
    private double capacity;

    // 车辆类型枚举（与示例一致）
    public enum VehicleType {
        BUS, LOGISTIC
    }

    public Vehicle(VehicleType type, double capacity) {
        this.type = type;
        this.capacity = capacity;
        this.id = type.name() + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    public Vehicle(String id, VehicleType type, double capacity) {
        this.id = id; // 直接使用传入的ID（如 LOG_VEHICLE_1）
        this.type = type;
        this.capacity = capacity;
    }

    private String generateId(VehicleType type) {
        return type + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getId() { return id; }
    public VehicleType getType() { return type; }
    public void setType(VehicleType type) { this.type = type; }
    public double getCapacity() { return capacity; }
    public void setCapacity(double capacity) { this.capacity = capacity; }

    @Override
    public String toString() {
        return "Vehicle{" + id + ", type=" + type + ", capacity=" + capacity + "}";
    }
}