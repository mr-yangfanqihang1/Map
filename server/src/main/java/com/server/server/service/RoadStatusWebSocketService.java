package com.server.server.service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.server.server.data.RoadUpdateMessage;

@Service
public class RoadStatusWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    public RoadStatusWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    
    public void notifyUser(int userId, long roadId, double durationAdjustment) {
        RoadUpdateMessage message = new RoadUpdateMessage(roadId, durationAdjustment);
        System.out.println("Sending message to /user/" + userId + "/queue/roadUpdates: " + message);
        try{messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/roadUpdates", message);}
        catch (Exception e) {System.out.println("error webSocket sending message");}
    }
    
}
