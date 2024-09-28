package com.server.server.controller;
import com.server.server.service.*;
import com.server.server.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/dispatch")
public class DispatchLogController {
    @Autowired
    private DispatchLogService dispatchLogService;

    @PostMapping
    public void createDispatchLog(@RequestBody DispatchLog log) {
        dispatchLogService.createDispatchLog(log);
    }

    @GetMapping
    public List<DispatchLog> getAllLogs() {
        return dispatchLogService.getAllLogs();
    }
}