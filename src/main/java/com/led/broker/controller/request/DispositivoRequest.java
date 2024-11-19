package com.led.broker.controller.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispositivoRequest {

    private String mac;
    private String nome;
    private boolean ignorarAgenda;
    private String latitude;
    private String longitude;
    private ConfiguracaoRequest configuracao;
}
