package com.server.server.controller;
import com.server.server.service.*;
import com.server.server.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/roads")
public class RoadController {
    @Autowired
    private RoadService roadService;

    @PostMapping
    public void createRoad(@RequestBody Road road) {
        roadService.createRoad(road);
    }

    @GetMapping
    public List<Road> getAllRoads() {
        return roadService.getAllRoads();
    }

    @GetMapping("/{id}")
    public Road getRoadById(@PathVariable int id) {
        return roadService.getRoadById(id);
    }
}