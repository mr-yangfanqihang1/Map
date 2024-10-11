package com.server.server.service;
import com.server.server.data.*;
import com.server.server.mapper.*;


import org.mybatis.spring.MyBatisSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
//import java.util.List;

@Service
public class RouteService {
    @Autowired
    private RouteMapper routeMapper;

    public void createRoute(Route route) {
        try{
            System.out.println("Insert Route : " + route+"\n");
            long start = System.currentTimeMillis();
            routeMapper.insertRoute(route);
            long end = System.currentTimeMillis();
            System.out.println("Insert Route : " + route+"\n"+"Add to queue time: "+(end-start)+"ms");
        } catch (MyBatisSystemException e) {
            e.printStackTrace();
        }
    }
    
    public Route getRouteById(int id) {
        return routeMapper.getRouteById(id);
    }
}

