package com.server.server.controller;

import com.server.server.service.RouteServiceImpl;
import com.server.server.service.SmartService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/smart")
public class SmartController {
    private SmartService smartService;
    private RouteServiceImpl routeServiceImpl;
    public boolean SmartGetIsStatus(){
        boolean isStatus = smartService.getIsStatus();
        routeServiceImpl.setIsStatus();
        return isStatus;
    }

}
