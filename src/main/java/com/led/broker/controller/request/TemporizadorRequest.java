package com.led.broker.controller.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
public class TemporizadorRequest {

    private UUID idCor;
    private String mac;
    private boolean cancelar;

}
