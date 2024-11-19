package com.led.broker.controller.response;

import com.led.broker.model.constantes.Efeito;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class ComandoRequest {

    private Efeito efeito;
    private int[] cor;
    private int leds;
    private int faixa;
    private int intensidade;
    private int[] correcao;
    private int velocidade;
    private boolean responder;
}
