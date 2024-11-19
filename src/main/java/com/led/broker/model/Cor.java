package com.led.broker.model;

import com.led.broker.model.constantes.Efeito;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "cores")
public class Cor {


    @Id
    private UUID id;
    private String nome;
    private Efeito efeito;
    private int[] cor;
    private String primaria;
    private String secundaria;
    private int[] correcao;
    private int velocidade;
    private long time;
    private boolean rapida;
    @Transient
    private boolean responder;

}
