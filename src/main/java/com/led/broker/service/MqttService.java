package com.led.broker.service;

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
                .setHeader(MqttHeaders.RETAINED, false)
                .build();

        System.out.println("Comando enviado para: " + message);
        mqttOutbound.handleMessage(mqttMessage);
    }
}
