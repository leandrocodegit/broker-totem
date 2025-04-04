package com.led.broker.controller;

import com.led.broker.service.AuthService;
import com.led.broker.service.ComandoService;
import com.led.broker.service.FirmwareService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/firmware")
@RequiredArgsConstructor
public class FirmwareController {

    private final FirmwareService firmwareService;
    private final ComandoService comandoService;
    private final AuthService authService;
    @Value("${host-firmware}")
    private String host;


    @PatchMapping
    public ResponseEntity<?> downloadFileTeste() {
        try {

            return ResponseEntity.ok()
                    .body("resource");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
    @GetMapping(value = "/update/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> atualizarFirmware(@PathVariable long id, @RequestParam("token") String token) {
         authService.validarToken(token);
        return  Flux.concat(
                Mono.just("ok"),
                comandoService.enviardComandoUpdateFirmware(id, host)
                        .timeout(Duration.ofSeconds(60))
                        .onErrorResume(e -> Mono.just("Dispositivo " + id + " não respondeu")));
    }

    @PostMapping(value = "/upload/{id}", consumes = "multipart/form-data")
    public Mono<ResponseEntity<Map<String, String>>> uploadFile(@PathVariable long id, @RequestPart("file") Mono<FilePart> filePartMono, @RequestParam("token") String token) {
         authService.validarToken(token);
        return filePartMono
                .flatMap(filePart -> firmwareService.storeFile(String.valueOf(id), Mono.just(filePart)))
                .map(newFileName -> {
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Arquivo salvo com sucesso");
                    response.put("id", newFileName);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Erro ao salvar arquivo");
                    errorResponse.put("details", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                });
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") String id) {
        try {
            Resource resource = firmwareService.loadFileAsResource(String.valueOf(id));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}
