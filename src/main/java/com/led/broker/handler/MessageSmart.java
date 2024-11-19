package com.led.broker.handler;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.SmartMessageConverter;

public class MessageSmart implements SmartMessageConverter {
    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass, Object conversionHint) {
        return new String((byte[]) message.getPayload());
    }

    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers, Object conversionHint) {
        return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
    }

    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass) {
        return new String((byte[]) message.getPayload());
    }

    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers) {
        return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
    }
}
