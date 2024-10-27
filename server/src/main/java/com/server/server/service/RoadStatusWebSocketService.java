package com.server.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.server.server.data.RoadUpdateMessage;

@Service
public class RoadStatusWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public RoadStatusWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyUser(int userId, long roadId, double durationAdjustment) {
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/roadUpdates", new RoadUpdateMessage(roadId, durationAdjustment));
    }
    
    

}
