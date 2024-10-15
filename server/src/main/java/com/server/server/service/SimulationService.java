package com.server.server.service;
import com.server.server.data.Road;
import com.server.server.data.RouteData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SimulationService {

    private double maxLoad;
    private List<RouteData> routeDataList;
    private int numberOfVehicles;
    private boolean enablePhilosophers;
    private int numberOfPhilosophers;

    @Autowired
    public SimulationService(double maxLoad, List<RouteData> routeDataList) {
        this.maxLoad = maxLoad;
        this.routeDataList = routeDataList;
    }

    public void updateSimulationParameters(double maxLoad, List<RouteData> routeDataList, int numberOfVehicles, boolean enablePhilosophers, int numberOfPhilosophers) {
        this.maxLoad = maxLoad;
        this.routeDataList = routeDataList;
        this.numberOfVehicles = numberOfVehicles;
        this.enablePhilosophers = enablePhilosophers;
        this.numberOfPhilosophers = numberOfPhilosophers;
    }

    public void startSimulation() {
        // 模拟服务的逻辑
        // 这里可以添加具体的模拟实现，例如：
        System.out.println("Starting simulation with parameters:");
        System.out.println("Max Load: " + maxLoad);
        System.out.println("Route Data List: " + routeDataList);
        System.out.println("Number of Vehicles: " + numberOfVehicles);
        System.out.println("Enable Philosophers: " + enablePhilosophers);
        System.out.println("Number of Philosophers: " + numberOfPhilosophers);
        // 模拟结束
        System.out.println("Simulation completed!");
    }
}


