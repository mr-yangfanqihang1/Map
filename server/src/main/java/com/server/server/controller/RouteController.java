package com.server.server.controller;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.data.Route;
import com.server.server.data.User;
import com.server.server.service.RouteService;
import com.server.server.service.UserService;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @Autowired
    private UserService userService;  // 用于获取用户信息

    // 添加日志记录器
    private static final Logger logger = LoggerFactory.getLogger(RouteController.class);

    @PostMapping("/calculate")
    public Route calculateRoute(@RequestBody Route route) {
        // 在接收到前端数据时打印 Route 信息
        logger.info("Received route data: " + route.toString());
        System.out.println("Received route data: " + route.toString());

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
        // 打印收到的创建路径的数据
        logger.info("Creating route with data: " + route.toString());

        routeService.createRoute(route);
    }
}