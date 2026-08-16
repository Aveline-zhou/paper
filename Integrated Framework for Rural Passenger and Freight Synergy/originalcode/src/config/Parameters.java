package config;

public class Parameters {

    private static Parameters instance = new Parameters();
    private double initialTemperature = 10;
    private double coolingRate = 0.95;
    private double endTemperature = 0.1;
    private int maxIterations = 10;
    private double busCapacity = 20;
    private double logisticCapacity = 100;
    private int logisticVehicleCount = 22; // 默认3辆物流车
    private double busServiceCost = 1;       // 公交车单位货物服务成本
    private double distanceCost = 0.1;     // 物流车单位距离成本
    private double stationConstructionCost = 5; // 服务站建设成本
    private double inventoryCost = 0.1;        // 库存持有成本
    private int maxTimeWindows = 3;
    private double vehicleSpeed = 20.0; // 车辆行驶速度 km/h
    private double customerServiceTime = 0.25; // 顾客点服务时间（小时）= 15分钟
    private double stationServiceTime = 0.5; // 服务站补货时间（小时）= 30分钟
    private double maxWorkingTime = 8.0; // 最大工作时间（小时）= 8小时
    private double timeWindowLength = 8.0; // 时段长度（小时）
    private double diversityParameter = 10.0;  // 最差移除算子的多样性参数

    private Parameters() {
    }

    public static Parameters getInstance() {
        return instance;
    }

    public double getMaxStationDistance() {
        // 默认值可以根据实际情况调整，比如100单位距离
        return 100.0;
    }
    public void initializeDefaultParameters() {
        // 模拟退火参数
        this.initialTemperature = 40000;
        this.coolingRate = 0.95;
        this.endTemperature = 0.1;
        this.maxIterations = 3000;
        // 车辆容量参数
        this.busCapacity = 100;
        this.logisticCapacity =200;

        // 物流车数量参数
        this.logisticVehicleCount = 22;

        // 成本参数
        this.busServiceCost = 0.8;       // 公交车单位货物服务成本
        this.distanceCost = 2.5;    // 物流车单位距离成本
        this.stationConstructionCost = 5; // 服务站建设成本
        this.inventoryCost = 0.01;        // 库存持有成本

        // 时段参数
        this.maxTimeWindows = 3;

        // 时间参数
        this.vehicleSpeed = 20.0; // km/h
        this.customerServiceTime = 0.2; // 1
        this.stationServiceTime = 0.1; // 单位小时
        this.maxWorkingTime = 8.0; // 8小时
        this.timeWindowLength = 8.0; // 时段长度8小时
        this.diversityParameter = 10.0;

    }

    // 模拟退火参数
    public double getInitialTemperature() { return initialTemperature; }
    public void setInitialTemperature(double initialTemperature) {
        this.initialTemperature = initialTemperature;
    }

    public double getCoolingRate() { return coolingRate; }
    public void setCoolingRate(double coolingRate) {
        this.coolingRate = coolingRate;
    }

    public double getEndTemperature() { return endTemperature; }
    public void setEndTemperature(double endTemperature) {
        this.endTemperature = endTemperature;
    }

    public int getMaxIterations() { return maxIterations; }
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    // 车辆容量参数
    public double getBusCapacity() { return busCapacity; }
    public void setBusCapacity(double busCapacity) {
        this.busCapacity = busCapacity;
    }

    public double getLogisticCapacity() { return logisticCapacity; }
    public void setLogisticCapacity(double logisticCapacity) {
        this.logisticCapacity = logisticCapacity;
    }

    // 物流车数量参数
    public int getLogisticVehicleCount() {
        return logisticVehicleCount;
    }
    public void setLogisticVehicleCount(int logisticVehicleCount) {
        this.logisticVehicleCount = logisticVehicleCount;
    }

    // 时间相关参数 Getters and Setters
    public double getVehicleSpeed() { return vehicleSpeed; }
    public void setVehicleSpeed(double vehicleSpeed) { this.vehicleSpeed = vehicleSpeed; }

    public double getCustomerServiceTime() { return customerServiceTime; }
    public void setCustomerServiceTime(double customerServiceTime) { this.customerServiceTime = customerServiceTime; }

    public double getStationServiceTime() { return stationServiceTime; }
    public void setStationServiceTime(double stationServiceTime) { this.stationServiceTime = stationServiceTime; }

    public double getMaxWorkingTime() { return maxWorkingTime; }
    public void setMaxWorkingTime(double maxWorkingTime) { this.maxWorkingTime = maxWorkingTime; }

    public double getTimeWindowLength() { return timeWindowLength; }
    public void setTimeWindowLength(double timeWindowLength) { this.timeWindowLength = timeWindowLength; }

    // 成本参数
    public double getBusServiceCost() { return busServiceCost; }
    public void setBusServiceCost(double busServiceCost) {
        this.busServiceCost = busServiceCost;
    }

    public double getDistanceCost() { return distanceCost; }
    public void setDistanceCost(double distanceCost) {
        this.distanceCost = distanceCost;
    }

    public double getStationConstructionCost() { return stationConstructionCost; }
    public void setStationConstructionCost(double stationConstructionCost) {
        this.stationConstructionCost = stationConstructionCost;
    }

    public double getInventoryCost() { return inventoryCost; }
    public void setInventoryCost(double inventoryCost) {
        this.inventoryCost = inventoryCost;
    }

    // 时段参数
    public int getMaxTimeWindows() { return maxTimeWindows; }
    public void setMaxTimeWindows(int maxTimeWindows) {
        this.maxTimeWindows = maxTimeWindows;
    }

    // 算法参数
    public double getDiversityParameter() { return diversityParameter; }
    public void setDiversityParameter(double diversityParameter) {
        this.diversityParameter = diversityParameter;
    }

}