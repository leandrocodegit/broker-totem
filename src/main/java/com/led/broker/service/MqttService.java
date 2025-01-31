package com.led.broker.service;

import com.google.gson.Gson;
import com.led.broker.config.MqttGateway;
import com.led.broker.controller.request.ComandoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MqttService {
    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);
    private final MqttGateway mqttGateway;

    public MqttService(MqttGateway mqttGateway) {
        this.mqttGateway = mqttGateway;
    }

    synchronized public void sendRetainedMessage(String topic, ComandoRequest comandoRequest) {
        logger.warn("Comando enviado para: " + topic);
        String message = new Gson().toJson(comandoRequest);
        mqttGateway.sendToMqtt(message, topic);
        logger.warn("Mensagem: " + message);
    }
}
