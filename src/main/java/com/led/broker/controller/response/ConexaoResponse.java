package com.led.broker.controller.response;

import com.led.broker.model.constantes.StatusConexao;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ConexaoResponse {

    private LocalDateTime ultimaAtualizacao;
    private StatusConexao status;
}
