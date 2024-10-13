package com.server.server.service;
import com.server.server.data.Route;

public interface RouteService {
    void createRoute(Route route);
    Route getRouteById(int id);
    Route calculateRoute(Route route); // 新增方法，用于路径计算
}
