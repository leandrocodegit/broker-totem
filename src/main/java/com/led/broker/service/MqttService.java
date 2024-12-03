package com.led.broker.service;

import com.google.gson.Gson;
import com.led.broker.config.MqttGateway;
import com.led.broker.controller.request.ComandoRequest;
import com.led.broker.controller.response.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class MqttService {

    private final MqttGateway mqttGateway;

    public MqttService(MqttGateway mqttGateway) {
        this.mqttGateway = mqttGateway;
    }

   synchronized public void sendRetainedMessage(String topic, ComandoRequest comandoRequest) {

       String message = new Gson().toJson(comandoRequest);
        System.out.println("Comando enviado para: " + message);
       mqttGateway.sendToMqtt(message, topic);
    }
}
