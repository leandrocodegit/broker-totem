package com.led.broker.controller;

import com.led.broker.controller.request.ConfiguracaoSendRequest;
import com.led.broker.model.constantes.Topico;
import com.led.broker.service.MqttService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

//@Controller
public class WebSocketController {

//    @Autowired
//    private MqttService mqttService;
//    private final SimpMessagingTemplate messagingTemplate;
//
//    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
//        this.messagingTemplate = messagingTemplate;
//    }
//
//    @MessageMapping("/device/configuracao")
//    @SendTo("/topic/dashboard")
//    public String handleMessage(@Payload String message) {
//        return "Recebido: " + message;
//    }
//
//    @MessageMapping("/device")
//    public String handleMessageDevice(@Payload ConfiguracaoSendRequest request) {
//        request.setResponder(false);
//        if(request.getDevice() != null && !request.getDevice().isEmpty())
//            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + request.getDevice(),request);
//        return "Recebido: " + request;
//    }
}
