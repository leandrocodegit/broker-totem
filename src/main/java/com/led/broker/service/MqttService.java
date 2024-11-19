package com.led.broker.service;

import com.google.gson.Gson;
import com.led.broker.controller.response.ComandoRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class MqttService {
    @Autowired
    private MessageHandler mqttOutbound;





    public void sendRetainedMessage(String topic, String message, boolean reter) {
        message = message.replaceAll("#", "");
        Message<String> mqttMessage = MessageBuilder.withPayload(message)
                .setHeader(MqttHeaders.TOPIC, topic)
                .setHeader(MqttHeaders.RETAINED, reter)
                .build();

        System.out.println("Comando enviado para: " + message);
        mqttOutbound.handleMessage(mqttMessage);
    }

    public void sendRetainedMessage(String topic, ComandoRequest comandoRequest) {

       String message = new Gson().toJson(comandoRequest);
        Message<String> mqttMessage = MessageBuilder.withPayload(message)
                .setHeader(MqttHeaders.TOPIC, topic)

                .build();

        System.out.println("Comando enviado para: " + message);
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
