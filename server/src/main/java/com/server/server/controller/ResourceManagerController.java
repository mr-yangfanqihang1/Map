package com.server.server.controller;
import com.server.server.service.*;
import com.server.server.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//银行家算法
@RestController
@RequestMapping("/resources")
public class ResourceManagerController {

    @Autowired
    private ResourceManagerService resourceManagerService;


    @PostMapping("/registerEntity")
    public String registerEntity(@RequestBody Map<String, Object> request) {
        Object entity = request.get("entity");
        int maxLoad = (Integer) request.get("maxLoad");
        resourceManagerService.registerEntity(entity, maxLoad);
        return "Entity registered successfully";
    }

    @PostMapping("/requestResources")
    public String requestResources(@RequestBody Map<String, Object> request) {
        Object entity = request.get("entity");
        List<Map<String, Object>> routeDataMapList = (List<Map<String, Object>>) request.get("routeData");
        List<RouteData> routeDataList = routeDataMapList.stream()
                .map(map -> {
                    double distance = (Double) map.get("distance");
                    double duration = (Double) map.get("duration");
                    return new RouteData(distance, duration,0);
                })
                .collect(Collectors.toList());
        try {
            resourceManagerService.requestResources(entity, routeDataList);
            return "Request granted";
        } catch (Exception e) {
            return "Request denied: " + e.getMessage();
        }
    }

    @PostMapping("/releaseResources")
    public String releaseResources(@RequestBody Map<String, Object> request) {
        Object entity = request.get("entity");
        List<Map<String, Object>> routeDataMapList = (List<Map<String, Object>>) request.get("routeData");
        List<RouteData> routeDataList = routeDataMapList.stream()
                .map(map -> {
                    double distance = (Double) map.get("distance");
                    double duration = (Double) map.get("duration");
                    return new RouteData(distance, duration,0);
                })
                .collect(Collectors.toList());
        resourceManagerService.releaseResources(entity, routeDataList);
        return "Resources released successfully";
    }
}
