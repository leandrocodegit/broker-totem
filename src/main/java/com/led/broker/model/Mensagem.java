package com.led.broker.model;

import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.Efeito;
import com.led.broker.model.constantes.TipoConexao;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Mensagem {

    private long id;
    private String versao;
    private Comando comando;
    private TipoConexao tipoConexao;
    private int pino;
    private Efeito efeito;
    private String brockerId;

}
