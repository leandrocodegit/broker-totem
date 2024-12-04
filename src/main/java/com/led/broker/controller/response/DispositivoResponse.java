package com.led.broker.controller.response;

import com.led.broker.model.Conexao;
import com.led.broker.model.Configuracao;
import com.led.broker.model.constantes.Comando;
import com.led.broker.util.TimeUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispositivoResponse {


    private String mac;
    private String nome;
    private String ip;
    private int memoria;
    private String versao;
    private boolean ignorarAgenda;
    private boolean ativo;
    private Conexao conexao;
    private String latitude;
    private String longitude;
    private Comando comando;
    private Configuracao configuracao;
    private CorResponse cor;
    private String enderecoComplento;
    private String enderecoCompleto;
    private boolean isTimer;
    private OperacaoResponse operacao;
    public boolean isTimer() {
        return TimeUtil.isTime(this);
    }
}
