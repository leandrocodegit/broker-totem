package com.led.broker.service;

import com.led.broker.controller.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "totem", url = "${totem-url}")
public interface AuthService {

    @GetMapping("/auth/valid")
    public UserResponse validarToken(@RequestHeader("Authorization") String authorization);
    @GetMapping("/auth/valid/integracao")
    public String validarTokenIntegracao(@RequestHeader("Authorization") String authorization);

}
