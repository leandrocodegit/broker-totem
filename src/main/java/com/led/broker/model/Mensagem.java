package com.led.broker.model;

import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.Efeito;
import com.led.broker.model.constantes.TipoConexao;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Builder
public class Mensagem {

    private long id;
    private String versao;
    private Comando comando;
    private TipoConexao tipoConexao;
    private int portas;
    private int pino;
    private List<Efeito> efeito;
    private String brockerId;


    @Override
    public String toString() {
        return "{" +
                "id='" + id + '\'' +
                ", versao='" + versao + '\'' +
                ", comando=" + comando +
                ", efeito=" + efeito +
                ", brockerId='" + brockerId + '\'' +
                '}';
    }
}
