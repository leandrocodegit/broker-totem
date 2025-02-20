package com.led.broker.service;


import com.google.gson.Gson;
import com.led.broker.model.Agenda;
import com.led.broker.model.Cor;
import com.led.broker.model.Dispositivo;
import com.led.broker.model.Log;
import com.led.broker.model.constantes.*;
import com.led.broker.repository.CorRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.repository.OperacaoRepository;
import com.led.broker.util.ComandoFormater;
import com.led.broker.util.ConfiguracaoUtil;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ComandoService {

    private static final Logger logger = LoggerFactory.getLogger(ComandoService.class);
    private final MqttService mqttService;
    private final DispositivoRepository dispositivoRepository;
    private final CorRepository corRepository;
    private final LogRepository logRepository;
    private final OperacaoRepository operacaoRepository;
    public static Map<String, MonoSink<String>> streams = new HashMap<>();


    public Mono<String> createMono(String mac) {
        return Mono.create(sink -> {
            streams.put(mac, sink);
        });
    }

    public Mono<String> enviardComandoTeste(String mac) {

        logger.warn("Comando de teste: " + mac);
        Dispositivo dispositivo = buscarPorMac(mac);

        Mono<String> mono = createMono(mac);

//        if (dispositivo != null && dispositivo.getConfiguracao() != null) {
//            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComandoTeste(dispositivo.getConfiguracao()));
//        }

        return mono;
    }

    public Mono<String> enviardComandoSincronizar(String mac, boolean responder, TipoConfiguracao tipoConfiguracao) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mac);

        if (!dispositivoOptional.isPresent()) {
            logger.error(mac + " não encontrado ou inativo ");
            return Mono.just(mac + " não encontrado ou inativo ");
        }

        if(dispositivoOptional.get().getConexao().getStatus().equals(StatusConexao.Offline)){
            return Mono.just( "Dispositivo " + mac +  " offline ");
        }

        Dispositivo dispositivo = dispositivoOptional.get();
        Mono<String> mono = createMono(mac);

        if(tipoConfiguracao.equals(TipoConfiguracao.LORA_WAN)){

        }

        if (dispositivo.isAtivo()) {
            if(tipoConfiguracao.equals(TipoConfiguracao.LED)){
                dispositivo.setCor(repararCor(dispositivo));
            }
            if (dispositivo.getCor() != null || !tipoConfiguracao.equals(TipoConfiguracao.LED)) {
                mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ComandoFormater.gerarCodigo(dispositivo, responder, tipoConfiguracao));
                if (!responder) {
                    return mono.just("ok");
                }
            } else {
                logger.error(mac + " não possui configuração de cor");
                return mono.just("não possui configuração de cor");
            }

        }
        return mono;
    }

    public Mono<String> enviardComandoUpdateFirmware(String mac, String host) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mac);

        if (!dispositivoOptional.isPresent()) {
            logger.error(mac + " não encontrado ou inativo ");
            return Mono.just(mac + " não encontrado ou inativo ");
        }

        Dispositivo dispositivo = dispositivoOptional.get();
        Mono<String> mono = createMono(mac);

     //   mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), "ConfiguracaoUtil.gerarComandoFirmware(host)");
        return mono;
    }

    public void enviardComandoRapido(Dispositivo dispositivo, boolean cancelar) {

        try {
            if (cancelar) {
                logger.warn("Cancelar comando rápido: " + dispositivo.getMac());
                dispositivo.setCor(repararCor(buscarPorMac(dispositivo.getMac())));
            }

            if (dispositivo.isAtivo() && dispositivo.getCor() != null) {
             //   mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ComandoFormater.gerarCodigo(dispositivo, true));
            }
        } catch (Exception err) {
            logger.error(err.getMessage());
        }

    }

    public Mono<String> enviardComandoRapido(Dispositivo dispositivo, boolean cancelar, boolean interno) {

        Mono<String> mono = Mono.empty();

        if (!interno) {
            mono = createMono(dispositivo.getMac());
        }

        if (cancelar) {
            logger.warn("Cancelar comando rápido: " + dispositivo.getMac());
            dispositivo.setCor(repararCor(buscarPorMac(dispositivo.getMac())));
        }

        if (dispositivo.isAtivo() && dispositivo.getCor() != null) {
          //  mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ComandoFormater.gerarCodigo(dispositivo, true));
        }

        logger.warn("Comando rápido criado: " + dispositivo.getMac());
        return mono;
    }

    public String enviarComandoTodos(boolean responder,  String user) {

        try {
            List<Dispositivo> dispositivos = listaTodosDispositivos();
            logger.warn("Comando enviado para todos: " + dispositivos.size());

            if (!dispositivos.isEmpty()) {

                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario(user)
                        .mensagem("Todos")
                        .cor(null)
                        .comando(Comando.SINCRONIZAR)
                        .descricao(Comando.SINCRONIZAR.value())
                        .mac("Todos ativos")
                        .build());

                dispositivos.forEach(device -> {
                    if (device.isAtivo()) {
                        device.setCor(repararCor(device));
                    //    mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + device.getMac(), ComandoFormater.gerarCodigo(device, responder));
                    }
                });
              //  mqttService.sendRetainedMessage(Topico.DASHBOARD, "Atualizando dashboard todos");
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
            logger.error("Erro ao sincronizar");
            return "Sincronização não foi concluida";
        }
    }
    public static byte[] hexStringToByteArray(String hex) {
        int length = hex.length();
        byte[] data = new byte[length / 2]; // Cada 2 caracteres = 1 byte

        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
    private Cor repararCor(Dispositivo dispositivo) {
        if (dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.TEMPORIZADOR)) {
            if (TimeUtil.isTime(dispositivo)) {
                if (dispositivo.getOperacao().getCorTemporizador() != null) {
                    return dispositivo.getOperacao().getCorTemporizador();
                }
            }
        }

        if (Boolean.FALSE.equals(dispositivo.isIgnorarAgenda()) && dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.AGENDA)) {
            Agenda agenda = dispositivo.getOperacao().getAgenda();
            if (agenda != null && agenda.getCor() != null && agenda.isAtivo() && agenda.getDispositivos().contains(dispositivo.getMac())) {
                if(verificaSeAgendaValida(agenda, dispositivo.getMac()))
                    return agenda.getCor();
            }
        }

        dispositivo.getOperacao().setModoOperacao(ModoOperacao.DISPOSITIVO);
        operacaoRepository.save(dispositivo.getOperacao());
        return dispositivo.getCor();
    }

    public boolean verificaSeAgendaValida(Agenda agenda, String mac){

        if(!agenda.isAtivo() || agenda.getDispositivos() == null || agenda.getDispositivos().isEmpty())
            return false;
        var bool = agenda.getInicio().equals(LocalDate.now()) || agenda.getInicio().isBefore(LocalDate.now());
        if(bool)
            bool = agenda.getTermino().equals(LocalDate.now()) || agenda.getTermino().isAfter(LocalDate.now());;
        if(bool){
            return agenda.getDispositivos().contains(mac);
        }
        return false;
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
