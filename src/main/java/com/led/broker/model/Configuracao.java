package com.led.broker.model;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Configuracao {

    private int leds;
    private int intensidade;
    private int faixa;

}
