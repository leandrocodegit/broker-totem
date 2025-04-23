package com.led.broker.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.messaging.handler.annotation.SendTo;

import java.time.LocalDateTime;

@Getter
@SendTo
@Builder
public class TemporizadorControle {

    private Dispositivo dispositivo;
    private LocalDateTime timeout;
}
