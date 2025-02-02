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
    private boolean ativo;
    private boolean permiteComando;
    private Comando comando;
    private String latitude;
    private String longitude;
    private String brokerId;
    private String enderecoCompleto;
    private Configuracao configuracao;
    @DBRef
    private Operacao operacao;
    @DBRef
    private Conexao conexao;
    @DBRef
    private Cor cor;
}
