package com.led.broker.config;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;

@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
public interface MqttGateway {

    void sendToMqtt(String data, @Header("mqtt_topic") String topic);
    void sendToMqtt(byte[] data, @Header("mqtt_topic") String topic);
    void sendToMqtt(Message<byte[]> data);
}
