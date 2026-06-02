package com.led.broker.integracao.rest;

import com.led.broker.controller.response.UserResponse;
import com.led.broker.integracao.model.Dispositivo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(value = "dipositivo", url = "${api.dipositivo}")
public interface DispositivoRest {

    @GetMapping
    Dispositivo listaDispositivos();
    @GetMapping("/{id}")
    Dispositivo buscarDispositivo(
            @PathVariable UUID id);


}
