package com.led.broker.integracao.model;

import com.led.broker.model.*;
import com.led.broker.model.constantes.Comando;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
public class Dispositivo {

    private long id;
    private Long topico;
    private String nome;
    private String ip;
    private Integer memoria;
    private String versao;
    private boolean ignorarAgenda;
    private boolean permiteComando;
    private boolean ativo;
    private Comando comando;
    private String brokerId;
    private Endereco endereco;
    private String enderecoCompleto;
    private Float sensibilidadeVibracao;
    private String corVibracao;
    private Operacao operacao;
    private Conexao conexao;
    private Cor cor;
    private Agenda agenda;
    private Cliente cliente;

}
