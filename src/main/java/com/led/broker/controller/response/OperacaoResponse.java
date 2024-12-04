package com.led.broker.controller.response;

import com.led.broker.model.Agenda;
import com.led.broker.model.Cor;
import com.led.broker.model.constantes.ModoOperacao;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
public class OperacaoResponse {

    private ModoOperacao modoOperacao;
    private Agenda agenda;
    private Cor corTemporizador;
    private LocalDateTime time;
}