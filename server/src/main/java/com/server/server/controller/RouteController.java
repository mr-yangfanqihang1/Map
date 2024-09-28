package com.server.server.controller;
import com.server.server.service.*;
import com.server.server.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
//import java.util.List;
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
}
