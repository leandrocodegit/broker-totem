package com.led.broker.service;

import com.led.broker.model.KoreMensagem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "kore", url = "${kore-url}")
public interface KoreService {


    @PostMapping
    public String enviarMensagem(@RequestHeader("Authorization") String authorization, @RequestBody KoreMensagem mensagem);

}
