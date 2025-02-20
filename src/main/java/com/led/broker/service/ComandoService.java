package com.led.broker.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "comando", url = "${comando-url}")
public interface ComandoService {

    @GetMapping("/interno/{id}")
    public void sincronizar(@PathVariable("id") long id);
    @GetMapping("/interno/sincronizar/{user}/{responder}")
    public void sincronizarTodos(@PathVariable String  user, @PathVariable boolean responder);
}
