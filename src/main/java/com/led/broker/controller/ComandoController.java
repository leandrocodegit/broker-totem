package com.led.broker.controller;

import com.led.broker.controller.request.ParametroRequest;
import com.led.broker.controller.request.TemporizadorRequest;
import com.led.broker.service.ComandoService;
import com.led.broker.service.CorService;
import com.led.broker.service.DispositivoService;
import com.led.broker.service.MqttService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

@RestController
@RequestMapping("/comando")
@CrossOrigin("*")
public class ComandoController {

    @Autowired
    private ComandoService comandoService;
    @Autowired
    private DispositivoService dispositivoService;
    @Autowired
    private CorService corService;

    @GetMapping(value = "/sincronizar/{responder}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sincronizarTodos(@PathVariable boolean responder) {
        if(!responder) {
            Flux<String> devicesFlux = Flux.fromIterable(dispositivoService.listaTodosDispositivos());
            return devicesFlux.flatMap(mac ->
                    comandoService.enviardComandoSincronizar(mac, false)
                            .then(Mono.just("Comando enviado para " + mac))
            );
        }else{
            Flux<String> devicesFlux = Flux.fromIterable(dispositivoService.listaTodosDispositivos());
            return devicesFlux.flatMap(mac ->
                    comandoService.enviardComandoSincronizar(mac, true)
                            .timeout(Duration.ofSeconds(10))
                            .doOnNext(response -> System.out.println("Resposta recebida: " + response))
                            .onErrorResume(e -> Mono.just("Dispositivo " + mac + " não respondeu"))
            );
        }
    }

    @GetMapping(value = "/{mac}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<String> sincronizar(@PathVariable String mac) {
        return  Flux.concat(
                Mono.just(""),
                comandoService.enviardComandoSincronizar(mac, true)
                        .timeout(Duration.ofSeconds(10))
                        .onErrorResume(e -> Mono.just("Dispositivo " + mac + " não respondeu")));
    }

    @GetMapping("/temporizar/{idCor}/{mac}/{cancelar}")
    public Flux<String> temporizar(@PathVariable UUID idCor, @PathVariable String mac, @PathVariable boolean cancelar) {
       return Flux.concat(
                Mono.just("ok"),
                corService.salvarCorTemporizada(idCor, mac, cancelar)
                        .timeout(Duration.ofSeconds(10))
                        .onErrorResume(e -> Mono.just("Falha, não houve resposta")));
    }
 }
