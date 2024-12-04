package com.led.broker.model;

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
@Builder
@Document(collection = "operacoes")
public class Operacao {

    @Id
    private String mac;
    private ModoOperacao modoOperacao;
    @DBRef
    private Agenda agenda;
    @DBRef
    private Cor corTemporizador;
    private LocalDateTime time;
}
