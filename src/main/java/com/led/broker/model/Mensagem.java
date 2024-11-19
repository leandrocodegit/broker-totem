package com.led.broker.model;

import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.Efeito;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Mensagem {

    private String id;
    private String ip;
    private String versao;
    private int memoria;
    private Comando comando;
    private Efeito efeito;
    private int[] parametros;
    private String brockerId;

}
