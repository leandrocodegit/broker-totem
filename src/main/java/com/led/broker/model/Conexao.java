package com.led.broker.model;

import com.led.broker.model.constantes.ClasseLora;
import com.led.broker.model.constantes.StatusConexao;
import com.led.broker.model.constantes.TipoConexao;
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
    private String id;
    private LocalDateTime ultimaAtualizacao;
    private StatusConexao status;
    private TipoConexao tipoConexao;
    private Boolean habilitarWifi;
    private String ssid;
    private String senha;
    private Boolean habilitarLoraWan;
    private Integer modoLora;
    private ClasseLora classe;
    private String devEui;
    private String appEui;
    private String appKey;
    private String nwkSKey;
    private String appSKey;
    private String devAddr;
    private Integer txPower;
    private Integer dataRate;
    private Integer adr;
    private Integer snr;
    private Integer rssi;
    private Boolean autoJoin;

    public Integer getModoLora() {
        return modoLora == null ? 0 : modoLora;
    }

    public Integer getTxPower() {
        return txPower == null ? 0 : txPower;
    }

    public Integer getAdr() {
        return adr == null ? 0 : adr;
    }

    public Integer getSnr() {
        return snr == null ? 0 : snr;
    }

    public Integer getRssi() {
        return rssi == null ? 0 : rssi;
    }
}
