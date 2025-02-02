package com.led.broker.service;


import com.google.gson.Gson;
import com.led.broker.model.Agenda;
import com.led.broker.model.Cor;
import com.led.broker.model.Dispositivo;
import com.led.broker.model.Log;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.ModoOperacao;
import com.led.broker.model.constantes.StatusConexao;
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

    public void enviarComando(Agenda agenda) {

        if(agenda.getCor() != null) {
            List<Dispositivo> dispositivos = Collections.EMPTY_LIST;

            if (agenda.isTodos()) {
                dispositivos = dispositivoRepository.findAllByAtivoIgnorarAgendaOnline(true, false, Comando.ONLINE);
            } else {
                dispositivos = dispositivoRepository.findAllById(agenda.getDispositivos());
            }

            if (!dispositivos.isEmpty()) {
                dispositivos.forEach(device -> {

                    if (device.isAtivo() && device.getConexao().getStatus().equals(StatusConexao.Online) && device.getConfiguracao() != null) {
                        if (Boolean.FALSE.equals(device.isIgnorarAgenda()) && !TimeUtil.isTime(device)) {
                            device.setCor(agenda.getCor());
                            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + device.getMac(),
                                    new Gson().toJson(ConfiguracaoUtil.gerarComando(device)), false);
                            agendaDeviceService.atualizarOperacaoDispositivo(agenda, device);
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
                        .mac(agenda.getDispositivos().toString())
                        .build());
            }
        }
    }

    public Optional<Cor> buscaCor(UUID id) {
        return corRepository.findById(id);
    }

}
