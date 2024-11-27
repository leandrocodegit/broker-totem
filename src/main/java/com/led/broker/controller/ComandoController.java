package com.led.broker.controller;

import com.led.broker.service.AuthService;
import com.led.broker.service.ComandoService;
import com.led.broker.service.CorService;
import com.led.broker.service.DispositivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/comando")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ComandoController {

    private final ComandoService comandoService;
    private final DispositivoService dispositivoService;
    private final CorService corService;
    private final AuthService authService;

    @GetMapping(value = "/sincronizar/{responder}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sincronizarTodos(@PathVariable boolean responder, @RequestParam("token") String token) {
        if(!responder) {
            authService.validarToken(token);
            Flux<String> devicesFlux = Flux.fromIterable(dispositivoService.listaTodosDispositivos());
            return devicesFlux.flatMap(mac ->
                    comandoService.enviardComandoSincronizar(mac, false)
                            .then(Mono.just("Comando enviado para " + mac))
            );
        }else{
            authService.validarToken(token);
            Flux<String> devicesFlux = Flux.fromIterable(dispositivoService.listaTodosDispositivos());
            return devicesFlux.flatMap(mac ->
                    comandoService.enviardComandoSincronizar(mac, true)
                            .timeout(Duration.ofSeconds(10))
                            .doOnNext(response -> System.out.println("Resposta recebida: " + response))
                            .onErrorResume(e -> Mono.just("Dispositivo " + mac + " não respondeu"))
            );
        }
    }

    @GetMapping(value = "/{mac}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sincronizar(@PathVariable String mac, @RequestParam("token") String token) {
        authService.validarToken(token);
        return  Flux.concat(
                Mono.just("ok"),
                comandoService.enviardComandoSincronizar(mac, true)
                        .timeout(Duration.ofSeconds(10))
                        .onErrorResume(e -> Mono.just("Dispositivo " + mac + " não respondeu")));
    }

    @GetMapping("/temporizar/{idCor}/{mac}")
    public Flux<String> temporizar(@PathVariable UUID idCor, @PathVariable String mac, @RequestParam("token") String token) {
        authService.validarToken(token);
        return Flux.concat(
                Mono.just("ok"),
                corService.salvarCorTemporizada(idCor, mac, false)
                        .timeout(Duration.ofSeconds(10))
                        .onErrorResume(e -> Mono.just("Falha, não houve resposta")));
    }

    @GetMapping("/temporizar/{mac}")
    public Flux<String> cancelarTemporizar(@PathVariable String mac, @RequestParam("token") String token) {
        authService.validarToken(token);
        return Flux.concat(
                Mono.just("ok"),
                corService.salvarCorTemporizada(null, mac, true)
                        .timeout(Duration.ofSeconds(10))
                        .onErrorResume(e -> Mono.just("Falha, não houve resposta")));
    }

    @GetMapping(value = "/teste/{mac}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> testar(@PathVariable String mac, @RequestParam("token") String token) {
        authService.validarToken(token);
        return  Flux.concat(
                Mono.just("ok"),
                comandoService.enviardComandoTeste(mac)
                        .timeout(Duration.ofSeconds(10))
                        .onErrorResume(e -> Mono.just("Dispositivo " + mac + " não respondeu")));
    }

    @GetMapping(value = "/interno/sincronizar", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sincronizarTodosInterno() {
        return Flux.concat(
                Flux.just("ok"),
                Flux.defer(() -> Flux.just(comandoService.enviarComandoTodos()))
        );
    }

    @GetMapping(value = "/interno/{mac}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sincronizarInterno(@PathVariable String mac) {
        return  Flux.concat(
                Mono.just("ok"),
                comandoService.enviardComandoSincronizar(mac, false));
    }


 }
