package com.led.broker.controller.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.led.broker.model.Configuracao;
import com.led.broker.model.Temporizador;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.StatusConexao;
import com.led.broker.util.TimeUtil;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DispositivoDashResponse {


    private String mac;
    private String nome;
    private String ip;
    private int memoria;
    private String versao;
    private boolean ignorarAgenda;
    private boolean ativo;
    private StatusConexao conexao;
    private String latitude;
    private String longitude;
    private Comando comando;
    private Configuracao configuracao;
    private CorResponse cor;
    private String enderecoComplento;
    private String enderecoCompleto;
    private boolean isTimer;
//    @JsonIgnore
//    private Temporizador temporizador;
//    public boolean isTimer() {
//        return TimeUtil.isTime(this);
    }
}
