package com.led.broker.model;

import com.led.broker.model.constantes.Efeito;
import com.led.broker.model.constantes.TipoCor;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Parametro {

    private int pino;
    private Efeito efeito;
    private int[] cor;
    private String primaria;
    private String secundaria;
    private int[] correcao;
    private Configuracao configuracao;
}
