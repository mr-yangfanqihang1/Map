package com.server.server.controller;

import com.server.server.data.RoadUpdateMessage;
import com.server.server.service.RoadStatusWebSocketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/road")
public class WebSocketController {

    private final RoadStatusWebSocketService roadStatusWebSocketService;

    public WebSocketController(RoadStatusWebSocketService roadStatusWebSocketService) {
        this.roadStatusWebSocketService = roadStatusWebSocketService;
    }

    @PostMapping("/notify")
    public ResponseEntity<String> notifyUser(
            @RequestParam int userId,
            @RequestParam long roadId,
            @RequestParam double duration) {
        // 创建 RoadUpdateMessage 实例
        RoadUpdateMessage message = new RoadUpdateMessage(userId,roadId, duration);
        // 调用 WebSocket 服务来处理用户通知
        roadStatusWebSocketService.notifyUser(message);
        return ResponseEntity.ok("Notification sent to user " + userId);
    }

    @PostMapping("/notify-connection")
    public ResponseEntity<String> notifyConnectionSuccess(@RequestParam int userId) {
        // 调用 WebSocket 服务来通知连接成功
        roadStatusWebSocketService.notifyConnectionSuccess(userId);
        return ResponseEntity.ok("Connection success message sent to user " + userId);
    }
}

