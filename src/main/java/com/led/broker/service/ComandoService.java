package com.led.broker.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "comando", url = "${comando-url}")
public interface ComandoService {

    @GetMapping("/interno/{mac}")
    public void sincronizar(@PathVariable("mac") String mac);
    @GetMapping("/interno/sincronizar/{user}/{responder}")
    public void sincronizarTodos(@PathVariable String  user, @PathVariable boolean responder);
}
