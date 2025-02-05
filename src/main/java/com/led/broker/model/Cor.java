package com.led.broker.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
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
    private long time;
    private int quantidadePinos;
    private boolean rapida;
    private List<Parametro> parametros;
    @Transient
    private boolean responder;

}
