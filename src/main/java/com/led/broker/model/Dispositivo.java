package com.led.broker.model;

import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.StatusConexao;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Document(collection = "dispositivos")
public class Dispositivo {

    @Id
    private String mac;
    private String nome;
    private String ip;
    private Integer memoria;
    private String versao;
    private boolean ignorarAgenda;
    private LocalDateTime ultimaAtualizacao;
    private boolean ativo;
    private Comando comando;
    private String latitude;
    private String longitude;
    private String brokerId;
    private Endereco endereco;
    private String enderecoCompleto;
    private Temporizador temporizador;
    private Configuracao configuracao;
    private StatusConexao status;
    @DBRef
    private Cor cor;
    @DBRef
    private Agenda agenda;


}
