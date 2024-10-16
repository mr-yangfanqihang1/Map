package com.server.server.controller;
import com.server.server.service.*;
import com.server.server.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/traffic")
public class TrafficDataController {
    @Autowired
    private TrafficDataService trafficDataService;

    @PostMapping("/upload")
    public void uploadTrafficData(@RequestBody TrafficData trafficData) {
        trafficDataService.uploadTrafficData(trafficData);
    }


    @PostMapping("/update")
    public void updateTrafficData(@RequestBody TrafficData trafficData) {
        trafficDataService.updateTrafficData(trafficData);
    }

    @GetMapping("/road/{roadId}")
    public List<TrafficData> getTrafficDataByRoadId(@PathVariable int roadId) {
        return trafficDataService.getTrafficDataByRoadId(roadId);
    }

}