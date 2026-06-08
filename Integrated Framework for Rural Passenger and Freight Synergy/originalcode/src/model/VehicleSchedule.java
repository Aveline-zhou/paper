package model;
import config.Parameters;
import java.util.ArrayList;
import java.util.List;

/**
 * 车辆调度计划 - 管理车辆的时间安排和任务分配
 */
public class VehicleSchedule {
    private Vehicle vehicle;
    private List<RouteTask> tasks;
    private double currentTime; // 当前时间（从0开始）
    private double totalWorkingTime;

    public VehicleSchedule(Vehicle vehicle) {
        this.vehicle = vehicle;
        this.tasks = new ArrayList<>();
        this.currentTime = 0.0;
        this.totalWorkingTime = 0.0;
    }
    public boolean canAddTask(RouteTask task) {
        double taskEndTime = currentTime + task.getTotalTime();
        return taskEndTime <= Parameters.getInstance().getMaxWorkingTime();
    }

    public boolean addTask(RouteTask task) {
        if (canAddTask(task)) {
            task.setStartTime(currentTime);
            tasks.add(task);
            currentTime += task.getTotalTime();
            totalWorkingTime += task.getTotalTime();
            return true;
        }
        return false;
    }

    public double getCurrentTime() {
        return currentTime;
    }
    public double getTotalWorkingTime() {
        return totalWorkingTime;
    }
    public Vehicle getVehicle() {
        return vehicle;
    }
    public List<RouteTask> getTasks() {
        return new ArrayList<>(tasks);
    }
    public double getUtilization() {
        return totalWorkingTime / Parameters.getInstance().getMaxWorkingTime();
    }
    @Override
    public String toString() {
        return String.format("车辆 %s: %.2f/%.1f 小时 (利用率: %.1f%%)",
                vehicle.getId(), totalWorkingTime, Parameters.getInstance().getMaxWorkingTime(),
                getUtilization() * 100);
    }
}