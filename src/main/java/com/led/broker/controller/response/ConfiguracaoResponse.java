package com.led.broker.controller.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfiguracaoResponse {

    private int leds;
    private int intensidade;
    private int faixa;

}
