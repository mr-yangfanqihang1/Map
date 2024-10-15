package com.server.server.controller;

import com.server.server.data.Road;
import com.server.server.data.RouteData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class SimulationController {

    private final SimulationService simulationService;
    public Road road = new Road();
    public RouteData routeData = new RouteData();

    public SimulationController() {
        // 假设有一个服务来从数据表中获取max_load和routeData
        List<RouteData> routeDataList = getRouteDataFromDatabase();
        double maxLoad = getMaxLoadFromDatabase();

        // 初始化SimulationService时使用从数据库获取的数据
        simulationService = new SimulationService(maxLoad, routeDataList);
    }

    @GetMapping("/startSimulation")
    public String startSimulation(@RequestParam int numberOfVehicles, @RequestParam boolean enablePhilosophers,
                                  @RequestParam int numberOfPhilosophers) {
        // 修改SimulationService的参数，假设我们已经从数据库获取了max_load和routeData
        List<RouteData> updatedRouteDataList = getRouteDataFromDatabase();
        double updatedMaxLoad = getMaxLoadFromDatabase();

        // 使用新的参数更新SimulationService
        simulationService.updateSimulationParameters(updatedMaxLoad, updatedRouteDataList, numberOfVehicles, enablePhilosophers, numberOfPhilosophers);
        simulationService.startSimulation();
        return "Simulation started!";
    }

    // 假设的方法，用于从数据库中获取max_load
    private int getMaxLoadFromDatabase() {
        // 这里应该是查询数据库并获取max_load的逻辑
        int maxLoad = road.getMaxLoad();
        return maxLoad;
    }

    // 假设的方法，用于从数据库中获取routeData
    private List<RouteData> getRouteDataFromDatabase() {
        // 这里应该是查询数据库并获取routeData的逻辑
        double distance = routeData.getDistance();
        double duration = routeData.getDuration();
        return List.of(new RouteData(distance,duration,0)); // 示例值
    }
}

class SimulationService {
    private double maxLoad;
    private List<RouteData> routeDataList;
    private int numberOfVehicles;
    private boolean enablePhilosophers;
    private int numberOfPhilosophers;

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
    }
}







