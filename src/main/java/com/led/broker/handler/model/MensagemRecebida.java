package com.led.broker.handler.model;

import com.led.broker.model.constantes.Efeito;
import com.led.broker.model.constantes.TipoConexao;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensagemRecebida {

    private UUID id;
    private TipoMensagem tipo;
    private String versao;
    private String modelo;
    private boolean mcu;
    private boolean wifi;
    private boolean eth;
    private int rssi;
    private TipoConexao conexao;
    private List<Efeito> efeito;
    private String cor;
}
