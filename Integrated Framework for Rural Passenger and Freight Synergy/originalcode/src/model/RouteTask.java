package model;

import java.util.List;

/**
 * 路径任务 - 包含路径和时间信息
 */
public class RouteTask {
    private Route route;
    private double startTime;
    private double totalTime;
    private double travelTime;
    private double serviceTime;

    public RouteTask(Route route, double travelTime, double serviceTime) {
        this.route = route;
        this.travelTime = travelTime;
        this.serviceTime = serviceTime;
        this.totalTime = travelTime + serviceTime;
    }

    // Getters
    public Route getRoute() { return route; }
    public double getStartTime() { return startTime; }
    public double getTotalTime() { return totalTime; }
    public double getTravelTime() { return travelTime; }
    public double getServiceTime() { return serviceTime; }
    public double getEndTime() { return startTime + totalTime; }

    // Setter
    public void setStartTime(double startTime) { this.startTime = startTime; }

    /**
     * 获取任务详细信息
     */
    public String getTaskDetails() {
        // 使用StringBuilder构建路径字符串
        List<Node> nodes = route.getNodes();
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) path.append("→");
            path.append(nodes.get(i).getId());
        }

        return String.format("%.2f-%.2f小时: %s (行驶:%.2fh, 服务:%.2fh)",
                startTime, getEndTime(), path.toString(), travelTime, serviceTime);
    }

    @Override
    public String toString() {
        return getTaskDetails();
    }
}