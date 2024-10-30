package com.server.server.controller;

import com.server.server.service.RouteServiceImpl;
import com.server.server.service.SmartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/smart")
public class SmartController {
    @Autowired
    private SmartService smartService;

    @GetMapping("/getIsStatus")
    public boolean SmartGetIsStatus(){
        RouteServiceImpl.setIsStatus();
        return RouteServiceImpl.getIsStatus();
    }
    @GetMapping("/returnIsStatus")
    public boolean SmartReturnIsStatus(){
        RouteServiceImpl.returnIsStatus();
        return RouteServiceImpl.getIsStatus();
    }

}
