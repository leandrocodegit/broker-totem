package com.led.broker.service;


import com.led.broker.model.Agenda;
import com.led.broker.model.Cor;
import com.led.broker.model.Dispositivo;
import com.led.broker.model.constantes.Topico;
import com.led.broker.repository.CorRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.util.ConfiguracaoUtil;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComandoService {

    private final MqttService mqttService;
    private final DispositivoRepository dispositivoRepository;
    private final AgendaDeviceService agendaDeviceService;
    private final CorRepository corRepository;
    public static Map<String, MonoSink<String>> streams = new HashMap<>();


    public Mono<String> createMono(String mac) {
        return Mono.create(sink -> {
            streams.put(mac, sink);
        });
    }

    public void enviardComandoSincronizar(String mac, boolean responder) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mac);

        if (dispositivoOptional.isPresent()) {
            Dispositivo dispositivo = dispositivoOptional.get();
            if (dispositivo.isAtivo() && dispositivo.getConfiguracao() != null) {
                dispositivo.setCor(getCor(dispositivo));
                if (dispositivo.getCor() != null) {
                    mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComando(dispositivo, responder));
                }
            }
        }
    }

    public void enviardComandoSincronizar(Dispositivo dispositivo) {

        if (dispositivo != null) {
            if (dispositivo.isAtivo() && dispositivo.getConfiguracao() != null) {
                if (dispositivo.getCor() != null) {
                    mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComando(dispositivo, false));
                }
            }
        }
    }

    private Cor getCor(Dispositivo dispositivo) {
        Agenda agenda = null;

        if (TimeUtil.isTime(dispositivo)) {
            Optional<Cor> corOptional = buscaCor(dispositivo.getTemporizador().getIdCor());
            if (corOptional.isPresent()) {
                return corOptional.get();
            }
        }
        if (Boolean.FALSE.equals(dispositivo.isIgnorarAgenda())) {
            agenda = agendaDeviceService.buscarAgendaDipositivoPrevistaHoje(dispositivo.getMac());
        }
        if (agenda != null && agenda.getCor() != null) {
            return agenda.getCor();
        }
        return dispositivo.getCor();
    }
    public Optional<Cor> buscaCor(UUID id) {
        return corRepository.findById(id);
    }
}
