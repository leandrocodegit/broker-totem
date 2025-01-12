package com.led.broker.controller;

import com.led.broker.service.AuthService;
import com.led.broker.service.ComandoService;
import com.led.broker.service.CorService;
import com.led.broker.service.DispositivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/integracao")
@RequiredArgsConstructor
public class IntegracaoController {

    private final ComandoService comandoService;
    private final DispositivoService dispositivoService;
    private final CorService corService;
    private final AuthService authService;
    @Value("${time-expiration}")
    private long timeExpiratio;

    @GetMapping("/flux/temporizar/{idCor}/{mac}")
    public Flux<String> temporizarFlux(@PathVariable UUID idCor, @PathVariable String mac, @RequestParam("token") String token) {
        authService.validarTokenIntegracao(token);
        return Flux.concat(
                Mono.just("ok"),
                corService.salvarCorTemporizada(idCor, mac, false)
                        .timeout(Duration.ofSeconds(timeExpiratio))
                        .onErrorResume(e -> Mono.just("Falha, não houve resposta")));
    }

    @GetMapping("/flux/temporizar/{mac}")
    public Flux<String> cancelarTemporizarFlux(@PathVariable String mac, @RequestParam("token") String token) {
        authService.validarTokenIntegracao(token);
        return Flux.concat(
                Mono.just("ok"),
                corService.salvarCorTemporizada(null, mac, true)
                        .timeout(Duration.ofSeconds(timeExpiratio))
                        .onErrorResume(e -> Mono.just("Falha, não houve resposta")));
    }

    @GetMapping("/temporizar/{idCor}/{mac}")
    public String temporizar(@PathVariable UUID idCor, @PathVariable String mac, @RequestParam("token") String token) {
        authService.validarTokenIntegracao(token);
        return corService.salvarCorTemporizada(idCor, mac, false).just("Comando enviado").block();
    }

    @GetMapping("/temporizar/{mac}")
    public String cancelarTemporizar(@PathVariable String mac, @RequestParam("token") String token) {
        authService.validarTokenIntegracao(token);
        return corService.salvarCorTemporizada(null, mac, true).just("Comando enviado").block();
    }
}
