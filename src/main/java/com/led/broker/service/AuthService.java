package com.led.broker.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "totem", url = "http://totem-container:8081/totem")
public interface AuthService {

    @GetMapping("/auth/valid")
    public void validarToken(@RequestHeader("Authorization") String authorization);

}
