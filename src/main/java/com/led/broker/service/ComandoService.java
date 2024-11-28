package com.led.broker.service;


import com.google.gson.Gson;
import com.led.broker.model.Agenda;
import com.led.broker.model.Cor;
import com.led.broker.model.Dispositivo;
import com.led.broker.model.Log;
import com.led.broker.model.constantes.Comando;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComandoService {

    private final MqttService mqttService;
    private final DispositivoRepository dispositivoRepository;
    private final AgendaDeviceService agendaDeviceService;
    private final CorRepository corRepository;
    private final LogRepository logRepository;
    public static Map<String, MonoSink<String>> streams = new HashMap<>();

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

    public void enviarComando(Agenda agenda) {

        if(agenda.getCor() != null) {
            List<Dispositivo> dispositivos = Collections.EMPTY_LIST;

            if (agenda.isTodos()) {
                dispositivos = dispositivoRepository.findAllByAtivoIgnorarAgendaOnline(true, false, Comando.ONLINE);
            } else {
                dispositivos = agenda.getDispositivos()
                        .stream()
                        .filter(device -> (device.isAtivo() && device.getComando().equals(Comando.ONLINE) && device.getConfiguracao() != null && !device.isIgnorarAgenda()))
                        .collect(Collectors.toList());
            }

            if (!dispositivos.isEmpty()) {
                dispositivos.forEach(device -> {

                    if (device.isAtivo() && device.getConfiguracao() != null) {
                        if (Boolean.FALSE.equals(device.isIgnorarAgenda()) && !TimeUtil.isTime(device)) {
                            device.setCor(agenda.getCor());
                            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + device.getMac(),
                                    new Gson().toJson(ConfiguracaoUtil.gerarComando(device)), false);
                        }
                    }
                });
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario(Comando.SISTEMA.value())
                        .mensagem("Tarefa agenda executada")
                        .cor(agenda.getCor())
                        .comando(Comando.SISTEMA)
                        .descricao("Tarefa agenda executada")
                        .mac(agenda.getDispositivos().stream().map(mac -> mac.getMac()).collect(Collectors.toList()).toString())
                        .build());
            }
        }
    }

    public Optional<Cor> buscaCor(UUID id) {
        return corRepository.findById(id);
    }

}
