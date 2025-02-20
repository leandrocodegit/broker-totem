package com.led.broker.model;

import com.led.broker.model.constantes.StatusConexao;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Document(collection = "conexoes")
public class Conexao {

    @Id
    private String mac;
    private LocalDateTime ultimaAtualizacao;
    private StatusConexao status;
    private boolean habilitarWifi;
    private String ssid;
    private String senha;
    private boolean habilitarLoraWan;
    private int modoLora;
    private String classe;
    private String devEui;
    private String appEui;
    private String appKey;
    private String nwkSKey;
    private String appSKey;
    private String devAddr;

}
