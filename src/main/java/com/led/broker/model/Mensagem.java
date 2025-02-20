package com.led.broker.model;

import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.Efeito;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class Mensagem {

    private String id;
    private String ip;
    private String versao;
    private int memoria;
    private Comando comando;
    private Efeito efeito;
    private String brockerId;

    @Override
    public String toString() {
        return "{" +
                "id='" + id + '\'' +
                ", ip='" + ip + '\'' +
                ", versao='" + versao + '\'' +
                ", memoria=" + memoria +
                ", comando=" + comando +
                ", efeito=" + efeito +
                ", brockerId='" + brockerId + '\'' +
                '}';
    }
}
