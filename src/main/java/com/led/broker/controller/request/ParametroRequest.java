package com.led.broker.controller.request;

import com.led.broker.model.constantes.Comando;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class ParametroRequest {

    private Comando comando;
    private String usuario;
    private boolean responder;
    private String device;

}
