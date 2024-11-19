package com.led.broker.controller.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ConfiguracaoSendRequest {

    private CorRequest cor;
    private String device;

}
