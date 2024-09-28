package com.server.server.service;
import com.server.server.data.*;
import com.server.server.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
//import java.util.List;

@Service
public class RouteService {
    @Autowired
    private RouteMapper routeMapper;

    public void createRoute(Route route) {
        routeMapper.insertRoute(route);
    }

    public Route getRouteById(int id) {
        return routeMapper.getRouteById(id);
    }
}

