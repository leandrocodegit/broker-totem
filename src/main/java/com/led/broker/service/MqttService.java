package com.led.broker.service;

import com.google.gson.Gson;
import com.led.broker.controller.request.ComandoRequest;
import com.led.broker.controller.response.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MqttService {

    private final MessageHandler mqttOutbound;

    public void sendRetainedMessage(String topic, ComandoRequest comandoRequest) {

       String message = new Gson().toJson(comandoRequest);
        Message<String> mqttMessage = MessageBuilder.withPayload(message)
                .setHeader(MqttHeaders.TOPIC, topic)
                .setHeader(MqttHeaders.RETAINED, true)
                .build();

        System.out.println("Comando enviado para: " + message);
        mqttOutbound.handleMessage(mqttMessage);
    }
}
