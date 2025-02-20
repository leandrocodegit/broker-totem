package com.led.broker.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

@FunctionalInterface
public interface HandlerMessager {
    void handleMessage(Message<Byte[]> message) throws MessagingException;
}

