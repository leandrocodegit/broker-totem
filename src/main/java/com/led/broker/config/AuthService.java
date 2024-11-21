package com.led.broker.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "totem", url = "http://172.19.0.1:8000/totem")
public interface AuthService {

    @GetMapping("/auth/valid")
    public void validarToken(@RequestHeader("Authorization") String authorization);
    @GetMapping("/auth/ws")
    public void validarwebSocker(@RequestHeader("Authorization") String authorization);

}
