package com.server.server.controller;

import com.server.server.service.RouteService;
import com.server.server.data.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @PostMapping
    public void createRoute(@RequestBody Route route) {
        routeService.createRoute(route);
    }

    @GetMapping("/{id}")
    public Route getRouteById(@PathVariable int id) {
        return routeService.getRouteById(id);
    }

    // 新增接口，用于A*路径计算
    @PostMapping("/calculate")
    public Route calculateRoute(@RequestBody Route route) {
        return routeService.calculateRoute(route);
    }
}
