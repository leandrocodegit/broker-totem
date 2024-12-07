package com.led.broker.service;


import com.google.gson.Gson;
import com.led.broker.model.Agenda;
import com.led.broker.model.Cor;
import com.led.broker.model.Dispositivo;
import com.led.broker.model.Log;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.ModoOperacao;
import com.led.broker.model.constantes.Topico;
import com.led.broker.repository.CorRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.util.ConfiguracaoUtil;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ComandoService {

    private final MqttService mqttService;
    private final DispositivoRepository dispositivoRepository;
    private final CorRepository corRepository;
    private final LogRepository logRepository;
    public static Map<String, MonoSink<String>> streams = new HashMap<>();


    public Mono<String> createMono(String mac) {
        return Mono.create(sink -> {
            streams.put(mac, sink);
        });
    }

    public Mono<String> enviardComandoTeste(String mac) {
        Dispositivo dispositivo = buscarPorMac(mac);

        Mono<String> mono = createMono(mac);

        if (dispositivo != null && dispositivo.getConfiguracao() != null) {
            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComandoTeste(dispositivo.getConfiguracao()));
        }

        return mono;
    }

    public Mono<String> enviardComandoSincronizar(String mac, boolean responder) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mac);

        if (!dispositivoOptional.isPresent()) {
            return Mono.just(mac + " não encontrado ou inativo ");
        }

        Dispositivo dispositivo = dispositivoOptional.get();
        Mono<String> mono = createMono(mac);

        if (dispositivo.isAtivo() && dispositivo.getConfiguracao() != null) {
            dispositivo.setCor(getCor(dispositivo));
            if (dispositivo.getCor() != null) {
                mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComando(dispositivo, responder));
                if (!responder) {
                    return mono.just("ok");
                }
            } else {
                return mono.just("não possui configuração de cor");
            }

        }
        return mono;
    }

    public Mono<String> enviardComandoUpdateFirmware(String mac, String host) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mac);

        if (!dispositivoOptional.isPresent()) {
            return Mono.just(mac + " não encontrado ou inativo ");
        }

        Dispositivo dispositivo = dispositivoOptional.get();
        Mono<String> mono = createMono(mac);

        mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComandoFirmware(host));
        return mono;
    }

    public void enviardComandoRapido(Dispositivo dispositivo, boolean cancelar) {

        try {
            if (cancelar) {
                dispositivo.setCor(getCor(buscarPorMac(dispositivo.getMac())));
            }

            if (dispositivo.isAtivo() && dispositivo.getConfiguracao() != null && dispositivo.getCor() != null) {
                mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComando(dispositivo, true));
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

    }

    public Mono<String> enviardComandoRapido(Dispositivo dispositivo, boolean cancelar, boolean interno) {

        Mono<String> mono = Mono.empty();

        if (!interno) {
            mono = createMono(dispositivo.getMac());
        }

        if (cancelar) {
            dispositivo.setCor(getCor(buscarPorMac(dispositivo.getMac())));
        }

        if (dispositivo.isAtivo() && dispositivo.getConfiguracao() != null && dispositivo.getCor() != null) {
            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComando(dispositivo, true));
        }


        return mono;
    }

    public String enviarComandoTodos() {

        try {
            List<Dispositivo> dispositivos = listaTodosDispositivos();

            if (!dispositivos.isEmpty()) {

                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("request.getUsuario()")
                        .mensagem("Todos")
                        .cor(null)
                        .comando(Comando.SINCRONIZAR)
                        .descricao(Comando.SINCRONIZAR.value())
                        .mac("Todos ativos")
                        .build());

                dispositivos.forEach(device -> {
                    if (device.isAtivo() && device.getConfiguracao() != null) {
                        device.setCor(getCor(device));
                        mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + device.getMac(), ConfiguracaoUtil.gerarComando(device));
                        System.out.println(new Gson().toJson(device.getConfiguracao()));
                    }
                });
            } else {
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("request.getUsuario()")
                        .cor(null)
                        .mensagem("Nenhum dos dispositos estão ativos")
                        .comando(Comando.NENHUM_DEVICE)
                        .descricao(Comando.NENHUM_DEVICE.value())
                        .build());
            }
            return "Comando enviado para todos";
        } catch (Exception erro) {
            return "Sincronização não foi concluida";
        }
    }

    private Cor getCor(Dispositivo dispositivo) {

        if (dispositivo.getOperacao().equals(ModoOperacao.TEMPORIZADOR)) {
            if (TimeUtil.isTime(dispositivo)) {
                if (dispositivo.getOperacao().getCorTemporizador() != null) {
                    return dispositivo.getOperacao().getCorTemporizador();
                }
            }
        }

        if (Boolean.FALSE.equals(dispositivo.isIgnorarAgenda()) && dispositivo.getOperacao().equals(ModoOperacao.AGENDA)) {
            Agenda agenda = dispositivo.getOperacao().getAgenda();
            if (agenda != null && agenda.getCor() != null) {
                return agenda.getCor();
            }
        }

        return dispositivo.getCor();
    }

    public Optional<Cor> buscaCor(UUID id) {
        return corRepository.findById(id);
    }

    private Dispositivo buscarPorMac(String mac) {

        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mac);
        if (dispositivoOptional.isPresent()) {
            return dispositivoOptional.get();
        } else if (ComandoService.streams.containsKey(mac)) {
            ComandoService.streams.remove(mac).success(mac + " não encontrado ou inativo ");
        }
        return null;
    }

    private List<Dispositivo> listaTodosDispositivos() {
        return dispositivoRepository.findAllByAtivo(true);
    }

}
