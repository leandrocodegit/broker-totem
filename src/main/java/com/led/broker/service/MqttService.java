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

    public void sendRetainedMessage(String topic, String message, boolean reter) {
        message = message.replaceAll("#", "");
        Message<String> mqttMessage = MessageBuilder.withPayload(message)
                .setHeader(MqttHeaders.TOPIC, topic)
                .setHeader(MqttHeaders.RETAINED, true)
                .build();
        if (!reter)
            System.out.println("Comando enviado para: " + message);
        mqttOutbound.handleMessage(mqttMessage);
    }

    public void sendRetainedMessage(String topic, ComandoRequest comandoRequest) {

        String message = new Gson().toJson(comandoRequest);
        Message<String> mqttMessage = MessageBuilder.withPayload(message)
                .setHeader(MqttHeaders.TOPIC, topic)
                .setHeader(MqttHeaders.RETAINED, true)
                .build();

        System.out.println("Comando enviado para: " + message);
        mqttOutbound.handleMessage(mqttMessage);
    }

    public void sendDashboardMessage(DashboardResponse dashboardResponse) {

        String message = new Gson().toJson(dashboardResponse);
        Message<String> mqttMessage = MessageBuilder.withPayload(message)
                .setHeader(MqttHeaders.TOPIC, "dashboard")
                .setHeader(MqttHeaders.RETAINED, true)
                .build();

        System.out.println("Atualizando dashboard: " + message);
        mqttOutbound.handleMessage(mqttMessage);
    }

    public void removeRetainedMessage(String topic) {
        Message<String> emptyMessage = MessageBuilder.withPayload("") // Mensagem vazia
                .setHeader(MqttHeaders.TOPIC, topic)
                .setHeader(MqttHeaders.RETAINED, true) // Define a mensagem como retida
                .build();

        mqttOutbound.handleMessage(emptyMessage);
    }
}
