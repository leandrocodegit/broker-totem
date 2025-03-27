package com.led.broker.controller;

import com.led.broker.model.Log;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.TipoConfiguracao;
import com.led.broker.repository.LogRepository;
import com.led.broker.service.AuthService;
import com.led.broker.service.ComandoService;
import com.led.broker.service.CorService;
import com.led.broker.service.DispositivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/comando")
@RequiredArgsConstructor
public class ComandoController {

    private final ComandoService comandoService;
    private final DispositivoService dispositivoService;
    private final CorService corService;
    private final AuthService authService;
    private final LogRepository logRepository;
    @Value("${time-expiration}")
    private long timeExpiratio;

    @GetMapping(value = "/sincronizar/{responder}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sincronizarTodos(@PathVariable boolean responder, @RequestParam("token") String token) {
        if(!responder) {
            var user = authService.validarToken(token);
            Flux<Long> devicesFlux = Flux.fromIterable(dispositivoService.listaTodosDispositivos(user.getClienteId(), responder));
            logRepository.save(Log.builder()
                    .data(LocalDateTime.now())
                    .usuario(user.getNome())
                    .mensagem("Todos")
                    .cor(null)
                    .comando(Comando.SINCRONIZAR)
                    .descricao(Comando.SINCRONIZAR.value)
                    .id(0)
                    .build());
            return devicesFlux.flatMap(id ->
                    comandoService.enviardComandoSincronizar(id, false, TipoConfiguracao.LED)
                            .then(Mono.just("Comando enviado para " + id))
            );
        }else{
           var user = authService.validarToken(token);
            Flux<Long> devicesFlux = Flux.fromIterable(dispositivoService.listaTodosDispositivos(user.getClienteId(), false));
            logRepository.save(Log.builder()
                            .key(UUID.randomUUID())
                    .data(LocalDateTime.now())
                    .usuario(user.getEmail())
                    .mensagem("Todos")
                    .cor(null)
                    .comando(Comando.SINCRONIZAR)
                    .descricao(Comando.SINCRONIZAR.value)
                    .id(0)
                    .build());
            return devicesFlux.flatMap(mac ->
                    comandoService.enviardComandoSincronizar(mac, true, TipoConfiguracao.LED)
                            .timeout(Duration.ofSeconds(timeExpiratio))
                            .doOnNext(response -> System.out.println("Resposta recebida: " + response))
                            .onErrorResume(e -> Mono.just("Dispositivo " + mac + " não respondeu"))
            );
        }
    }

    @GetMapping(value = "/{id}/{tipoConfiguracao}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sincronizar(@PathVariable long id, @PathVariable TipoConfiguracao tipoConfiguracao, @RequestParam("token") String token) {
        var user = authService.validarToken(token);
        return  Flux.concat(
                Mono.just("ok"),
                comandoService.enviardComandoSincronizar(id, true, tipoConfiguracao, true)
                        .timeout(Duration.ofSeconds(timeExpiratio))
                        .onErrorResume(e -> Mono.just("Dispositivo " + id + " não respondeu")));
    }

    @GetMapping("/flux/temporizar/{idCor}/{id}")
    public Flux<String> temporizarFlux(@PathVariable UUID idCor, @PathVariable long id, @RequestParam("token") String token) {
        var user = authService.validarToken(token);
        return Flux.concat(
                Mono.just("ok"),
                corService.salvarCorTemporizada(idCor, id, true, false, user.getEmail())
                        .timeout(Duration.ofSeconds(timeExpiratio))
                        .onErrorResume(e -> Mono.just("Falha, não houve resposta")));
    }

    @GetMapping("/temporizar/{idCor}/{id}")
    public ResponseEntity<String> temporizar(@PathVariable UUID idCor, @PathVariable long id, @RequestParam("token") String token) {
        var user = authService.validarToken(token);
        corService.salvarCorTemporizadaReponse(idCor, id, false, true, user.getEmail());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/flux/temporizar/{id}")
    public Flux<String> cancelarTemporizarFlux(@PathVariable long id, @RequestParam("token") String token) {
        var user = authService.validarToken(token);
        return Flux.concat(
                Mono.just("ok"),
                corService.salvarCorTemporizada(null, id, true,true, user.getEmail())
                        .timeout(Duration.ofSeconds(timeExpiratio))
                        .onErrorResume(e -> Mono.just("Falha, não houve resposta")));
    }

    @GetMapping(value = "/teste/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> testar(@PathVariable long id, @RequestParam("token") String token) {
        authService.validarToken(token);
        return  Flux.concat(
                Mono.just("ok"),
                comandoService.enviardComandoTeste(id)
                        .timeout(Duration.ofSeconds(timeExpiratio))
                        .onErrorResume(e -> Mono.just("Dispositivo " + id + " não respondeu")));
    }

    @GetMapping("/temporizar/{id}")
    public ResponseEntity<String> cancelarTemporizar(@PathVariable long id, @RequestParam("token") String token) {
        var user = authService.validarToken(token.replace("Bearer ", ""));
        return  ResponseEntity.ok(corService.salvarCorTemporizada(null, id, true,true, user.getEmail()).just("Comando enviado com sucesso").block());
    }


    @GetMapping(value = "/interno/sincronizar/{user}/{responder}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sincronizarTodosInterno(@PathVariable boolean responder, @RequestHeader UUID clienteId, @PathVariable String user) {
        return Flux.concat(
                Flux.just("ok"),
                Flux.defer(() -> Flux.just(comandoService.enviarComandoTodos(clienteId, responder, user, null, TipoConfiguracao.LED, true)))
        );
    }

    @GetMapping(value = "/interno/sincronizar/{cor}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sincronizarTodosVibracaoInterno(@PathVariable UUID cor, @RequestHeader UUID clienteId) {
        return Flux.concat(
                Flux.just("ok"),
                Flux.defer(() -> Flux.just(comandoService.enviarComandoTodosVibracao(clienteId, cor)))
        );
    }

    @GetMapping(value = "/interno/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sincronizarInterno(@PathVariable long id) {
        return  Flux.concat(
                Mono.just("ok"),
                comandoService.enviardComandoSincronizar(id, false, TipoConfiguracao.LED));
    }
    @GetMapping(value = "/interno/{id}/{topico}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void sincronizarInternoTipo(@PathVariable long id, @PathVariable int topico) {
         comandoService.enviardComandoSincronizarId(id, topico);
    }

    @GetMapping("interno/temporizar/{idCor}/{id}")
    public ResponseEntity<String> temporizarInterno(@PathVariable UUID idCor, @PathVariable long id) {
        corService.salvarCorTemporizada(idCor, id, false,false, "Sistema");
        return ResponseEntity.ok().build();
    }
 }
