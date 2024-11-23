package com.led.broker.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "totem", url = "http://totem-container:8000/totem")
public interface AuthService {

    @GetMapping("/auth/valid")
    public Boolean validarToken(@RequestHeader("Authorization") String authorization);
    @GetMapping("/auth/ws")
    public Boolean validarwebSocker(@RequestHeader("Authorization") String authorization);

}
