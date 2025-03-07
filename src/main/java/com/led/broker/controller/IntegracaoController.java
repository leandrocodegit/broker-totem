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

    @GetMapping("/flux/temporizar/{idCor}/{id}")
    public Flux<String> temporizarFlux(@PathVariable UUID idCor, @PathVariable long id, @RequestParam("token") String token) {
        var user = authService.validarToken(token);
        return Flux.concat(
                Mono.just("ok"),
                corService.salvarCorTemporizada(idCor, id, false, user)
                        .timeout(Duration.ofSeconds(timeExpiratio))
                        .onErrorResume(e -> Mono.just("Falha, não houve resposta")));
    }

    @GetMapping("/flux/temporizar/{id}")
    public Flux<String> cancelarTemporizarFlux(@PathVariable long id, @RequestParam("token") String token) {
        var user = authService.validarToken(token);
        return Flux.concat(
                Mono.just("ok"),
                corService.salvarCorTemporizada(null, id, true, user)
                        .timeout(Duration.ofSeconds(timeExpiratio))
                        .onErrorResume(e -> Mono.just("Falha, não houve resposta")));
    }

    @GetMapping("/temporizar/{idCor}/{id}")
    public String temporizar(@PathVariable UUID idCor, @PathVariable long id, @RequestParam("token") String token) {
        var user = authService.validarToken(token);
        return corService.salvarCorTemporizada(idCor, id, false, user).just("Comando enviado").block();
    }

    @GetMapping("/temporizar/{id}")
    public String cancelarTemporizar(@PathVariable long id, @RequestParam("token") String token) {
        var user = authService.validarToken(token);
        return corService.salvarCorTemporizada(null, id, true, user).just("Comando enviado").block();
    }
}
