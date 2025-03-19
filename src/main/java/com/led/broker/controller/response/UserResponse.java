package com.led.broker.controller.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
public class UserResponse {

    private UUID id;
    private UUID clienteId;
    private String nomeCliente;
    private String nome;
    private String email;
    private Boolean status;
    private Boolean business;
    private Boolean principal;
}
