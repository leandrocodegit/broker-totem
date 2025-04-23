package com.led.broker.controller.request;

import com.led.broker.model.*;
import com.led.broker.model.constantes.Comando;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
public class DispositivoRequest {

    private long id;
    private Cor cor;
}
