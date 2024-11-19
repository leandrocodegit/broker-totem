package com.led.broker.controller.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfiguracaoRequest {

    private int leds;
    private int intensidade;
    private int faixa;

}
