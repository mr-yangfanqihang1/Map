package com.server.server.service;

import com.server.server.data.RouteData;
import com.server.server.data.Route;
import com.server.server.mapper.RouteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RouteServiceImpl implements RouteService {

    @Autowired
    private RouteMapper routeMapper;

    @Override
    public void createRoute(Route route) {
        routeMapper.insertRoute(route);
    }

    @Override
    public Route getRouteById(int id) {
        return routeMapper.getRouteById(id);
    }

    @Override
    public Route calculateRoute(Route route) {
        double totalDistance = 0.0;
        double totalDuration = 0.0;

        // 迭代 routeData 中的路径点，计算总的距离和时间
        for (RouteData routeData : route.getRouteData()) {
            double distance = routeData.getDistance();  // 获取路径点的距离
            double duration = routeData.getDuration();  // 获取路径点的时间

            // 使用权重进行加权计算
            totalDistance += route.getWeightDistance() * distance;
            totalDuration += route.getWeightDuration() * duration;
        }

        // 将计算结果设置到 route 对象
        route.setDistance(String.valueOf(totalDistance));
        route.setDuration(String.valueOf(totalDuration));

        return route;
    }
}

