package com.server.server.controller;

import com.server.server.service.SmartService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/smart")
public class SmartController {
    private SmartService smartService;
    public boolean SmartGetIsStatus(){
        return smartService.getIsStatus();
    }

}
