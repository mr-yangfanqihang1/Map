package com.server.server.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import com.server.server.data.RoadUpdateMessage;

@Service
public class RoadStatusWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public RoadStatusWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/notify")
    public void notifyUser(@Payload RoadUpdateMessage message) {
        System.out.println("Received notification: " + message);
        // 处理消息逻辑
        messagingTemplate.convertAndSendToUser(String.valueOf(message.getUserId()), "/queue/roadUpdates", message);
    }

    public void notifyConnectionSuccess(int userId) {
        String message = "Connection successful";
        System.out.println("Sending connection success message to /user/" + userId + "/queue/roadUpdates: " + message);
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/roadUpdates", message);
        } catch (Exception e) {
            System.out.println("Error sending connection success message via WebSocket: " + e.getMessage());
        }
    }
}
