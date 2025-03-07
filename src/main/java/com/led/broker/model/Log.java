package com.led.broker.model;

import com.led.broker.model.constantes.Comando;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@Document(collection = "logs")
public class Log {

    @Id
    private UUID key;
    private LocalDateTime data;
    private String descricao;
    private Comando comando;
    private String usuario;
    private String mensagem;
    private long id;
    private Cor cor;

}
