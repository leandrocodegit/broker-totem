package com.led.broker.controller.request;

import com.led.broker.model.Dispositivo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Validated
public class AgendaRequest {

    private UUID id;
    private String nome;
    private boolean ativo;
    private LocalDate inicio;
    private LocalDate termino;
    private CorRequest cor;
    private List<Dispositivo> dispositivos;
    private boolean todos;
}
