package com.server.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.server.server.data.Route;
import com.server.server.service.RouteService;

@Component
public class RouteTestRunner implements CommandLineRunner {

    @Autowired
    private RouteService routeService;  // 注入 RouteService 用于执行寻路逻辑

    @Override
    public void run(String... args) throws Exception {
        // 从数据库中获取一条测试路径数据
        Route testRoute = getTestRouteFromDB();

        // 如果从数据库中获取的数据为空，输出错误信息
        if (testRoute == null) {
            System.out.println("测试路径未找到");
            return;
        }

        // 调用 A* 算法来计算路径
        Route calculatedRoute = routeService.calculateRoute(testRoute);

        // 在控制台打印寻路的结果
        System.out.println("寻路结果：");
        System.out.println(calculatedRoute.toString());
    }

    // 模拟从数据库中获取 Route 的方法
    private Route getTestRouteFromDB() {
        // 这里可以通过 RouteService 或者直接使用 JPA 获取数据库中的数据
        // 比如从 RouteRepository 获取特定 ID 的 Route 数据
        return routeService.getRouteById(1);  // 假设获取 ID 为 1 的 Route 数据
    }
}

