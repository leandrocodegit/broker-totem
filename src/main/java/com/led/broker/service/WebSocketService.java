package com.led.broker.service;

import com.led.broker.controller.response.DashboardResponse;
import com.led.broker.controller.response.DispositivoResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendMessageDashboard(DashboardResponse message) {
        messagingTemplate.convertAndSend("/topic/dashboard", message);
    }
    public void sendMessageDipositivos(List<DispositivoResponse> message) {
        messagingTemplate.convertAndSend("/topic/dispositivos", message);
    }
}