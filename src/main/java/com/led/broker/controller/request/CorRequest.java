package com.led.broker.controller.request;

import com.led.broker.model.constantes.Efeito;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CorRequest {

    private UUID id;
    private String nome;
    private Efeito efeito;
    private int[] cor;
    private String primaria;
    private String secundaria;
    private int[] correcao;
    private int velocidade;
    private long time;
    private boolean rapida;
    private boolean responder;
    private String mac;

}
