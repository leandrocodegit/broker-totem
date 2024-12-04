package com.led.broker.model;

import com.led.broker.model.constantes.ModoOperacao;
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
public class Operacao {

    private ModoOperacao modoOperacao;
    private Agenda agenda;
    private Cor corTemporizador;
    private LocalDateTime time;
}
