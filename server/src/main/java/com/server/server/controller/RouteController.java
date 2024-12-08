package com.server.server.controller;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> calculateRoute(@RequestBody Route route) {
        // 在接收到前端数据时打印 Route 信息
        logger.info("Received route data: " + route.toString());
        System.out.println("Received route data: " + route.toString());

        // 获取用户ID并查找用户信息
        User user = userService.getUserById(route.getUserId());

        if (user == null) {
            // 返回 404 状态和错误消息
            logger.error("用户不存在: " + route.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在");
        }

        // 设置请求时间和初始优先级
        route.setRequestTime(LocalDateTime.now());
        route.setPriority(route.getPriority());  // 假设用户有一个初始优先级

        try {
            // 调用 RouteService 计算路径
            Route calculatedRoute = routeService.calculateRoute(route);
            //延迟100s
            try {
                Thread.sleep(121800); // 暂停100秒
                } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                }
            return ResponseEntity.ok(calculatedRoute);
        } catch (RuntimeException e) {
            // 捕获计算路径时的异常并返回 404 状态和错误消息
            logger.error("计算路径失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
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
