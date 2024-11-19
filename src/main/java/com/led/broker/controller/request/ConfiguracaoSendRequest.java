package com.led.broker.controller.request;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ConfiguracaoSendRequest extends ComandoRequest {

    private String device;
}
