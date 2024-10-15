package com.server.server.controller;

import com.server.server.data.Route;
import com.server.server.data.User;
import com.server.server.service.RouteService;
import com.server.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @Autowired
    private UserService userService;  // 用于获取用户信息

    @PostMapping("/calculate")
    public Route calculateRoute(@RequestBody Route route) {
        // 获取用户ID并查找用户信息
        User user = userService.getUserById(route.getUserId());

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 设置请求时间和初始优先级
        route.setRequestTime(LocalDateTime.now());
        route.setPriority(route.getPriority());  // 假设用户有一个初始优先级

        // 调用 RouteService 计算路径
        return routeService.calculateRoute(route);
    }

    @GetMapping("/{id}")
    public Route getRouteById(@PathVariable int id) {
        return routeService.getRouteById(id);
    }

    @PostMapping("/create")
    public void createRoute(@RequestBody Route route) {
        routeService.createRoute(route);
    }
}
