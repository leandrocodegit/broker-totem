package com.led.broker.service;


import com.google.gson.Gson;
import com.led.broker.model.*;
import com.led.broker.model.constantes.*;
import com.led.broker.repository.CorRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.repository.OperacaoRepository;
import com.led.broker.util.ComandoFormater;
import com.led.broker.util.ConfiguracaoUtil;
import com.led.broker.util.CorUtil;
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
    private final CorUtil corUtil;
    public static Map<Long, MonoSink<String>> streams = new HashMap<>();


    public Mono<String> createMono(long id) {
        return Mono.create(sink -> {
            streams.put(id, sink);
        });
    }

    public Mono<String> enviardComandoTeste(long id) {

        logger.warn("Comando de teste: " + id);
        Dispositivo dispositivo = buscarPorId(id);

        Mono<String> mono = createMono(id);

//        if (dispositivo != null && dispositivo.getConfiguracao() != null) {
//            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComandoTeste(dispositivo.getConfiguracao()));
//        }

        return mono;
    }

    public void enviardComandoSincronizarId(long id, int topico) {

        if (topico <= 1000) {
            var dispositivo = dispositivoRepository.findAllByIdAndTopico(id, topico);
            if (!dispositivo.isPresent()) {
                mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + topico, ComandoFormater.gerarConfiguracaoId(id));
            }
        }
    }

    public Mono<String> enviardComandoSincronizar(long id, boolean responder, TipoConfiguracao tipoConfiguracao) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(id);

        if (!dispositivoOptional.isPresent()) {
            logger.error(id + " não encontrado ou inativo ");
            return Mono.just(id + " não encontrado ou inativo ");
        }

        if (dispositivoOptional.get().getConexao().getStatus().equals(StatusConexao.Offline)) {
            return Mono.just("Dispositivo " + id + " offline ");
        }

        Dispositivo dispositivo = dispositivoOptional.get();
        Mono<String> mono = createMono(id);

        if (tipoConfiguracao.equals(TipoConfiguracao.LIMPAR_FLASH)) {
            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getId(), ComandoFormater.gerarCodigoErase(dispositivo));
            return mono;
        }
        if (tipoConfiguracao.equals(TipoConfiguracao.WIFI)) {
            if(dispositivo.getConexao().getHabilitarWifi() == null || dispositivo.getConexao().getHabilitarWifi() == Boolean.FALSE)
                return Mono.just("Erro, WiFi não está habilitado");
            if(dispositivo.getConexao().getSsid() == null || dispositivo.getConexao().getSsid().isEmpty())
                return Mono.just("Erro, SSID é obrigatório");
            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getId(), ComandoFormater.gerarCodigoWIFI(dispositivo));
            return mono;
        }
        if (tipoConfiguracao.equals(TipoConfiguracao.LORA_WAN) || tipoConfiguracao.equals(TipoConfiguracao.LORA_WAN_PARAM)) {
            if(dispositivo.getConexao().getHabilitarLoraWan() == null || dispositivo.getConexao().getHabilitarLoraWan() == Boolean.FALSE)
                return Mono.just("Erro, LoraWan não está habilitado");
            if (tipoConfiguracao.equals(TipoConfiguracao.LORA_WAN) && dispositivo.getConexao().getClasse() == null)
                return Mono.just("Erro, Classe LoraWan é obrigatória");
            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getId(), ComandoFormater.gerarCodigoLora(dispositivo, true, tipoConfiguracao));
            return mono;
        }

        if (dispositivo.isAtivo()) {
            if (tipoConfiguracao.equals(TipoConfiguracao.LED) || tipoConfiguracao.equals(TipoConfiguracao.LED_RESTART)) {
                dispositivo.setCor(corUtil.repararCor(dispositivo));
            }
            if (dispositivo.getCor() != null || (!tipoConfiguracao.equals(TipoConfiguracao.LED) && !tipoConfiguracao.equals(TipoConfiguracao.LED_RESTART))) {
                mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getId(), ComandoFormater.gerarCodigo(dispositivo, responder, tipoConfiguracao));
                if (!responder) {
                    return mono.just("ok");
                }
            } else {
                logger.error(id + " não possui configuração de cor");
                return mono.just("não possui configuração de cor");
            }

        }
        return mono;
    }

    public Mono<String> enviardComandoUpdateFirmware(long id, String host) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(id);

        if (!dispositivoOptional.isPresent()) {
            logger.error(id + " não encontrado ou inativo ");
            return Mono.just(id + " não encontrado ou inativo ");
        }

        Dispositivo dispositivo = dispositivoOptional.get();
        Mono<String> mono = createMono(id);

        //   mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), "ConfiguracaoUtil.gerarComandoFirmware(host)");
        return mono;
    }

    public Mono<String> enviardComandoRapido(Dispositivo dispositivo, boolean cancelar, boolean interno) {

        Mono<String> mono = Mono.empty();

        if (!interno) {
            mono = createMono(dispositivo.getId());
        }

        if (cancelar) {
            logger.warn("Cancelar comando rápido: " + dispositivo.getId());
            dispositivo.setCor(corUtil.repararCor(buscarPorId(dispositivo.getId())));
        } else {
            dispositivo.setCor(corUtil.parametricarCorDispositivo(dispositivo.getCor(), dispositivo));
        }

        if (dispositivo.isAtivo() && dispositivo.getCor() != null) {
            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getId(), ComandoFormater.gerarCodigoCor(dispositivo, true, TipoConfiguracao.LED));
        }

        logger.warn("Comando rápido criado: " + dispositivo.getId());
        return mono;
    }

    public String enviarComandoTodosVibracao(UUID cor) {
        enviarComandoTodos(false, "Sistema", cor, TipoConfiguracao.VIBRACAO, true);
        logger.warn("Sincronizando cor de vibração " + cor.toString());
        return "Sincrolização enviada";
    }

    public String enviarComandoTodos(boolean responder, String user, UUID cor, TipoConfiguracao tipoConfiguracao, boolean interno) {

        try {
            List<Dispositivo> dispositivos = listaTodosDispositivos(tipoConfiguracao, cor);
            logger.warn("Comando enviado para todos: " + dispositivos.size());

            if (!dispositivos.isEmpty()) {
                if (!interno)
                    logRepository.save(Log.builder()
                            .key(UUID.randomUUID())
                            .data(LocalDateTime.now())
                            .usuario(user)
                            .mensagem("Todos")
                            .cor(null)
                            .comando(Comando.SINCRONIZAR)
                            .descricao(Comando.SINCRONIZAR.value)
                            .id(0)
                            .build());

                dispositivos.forEach(device -> {
                    if (device.isAtivo()) {
                        device.setCor(corUtil.repararCor(device));
                        mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + device.getId(), ComandoFormater.gerarCodigo(device, responder, tipoConfiguracao));
                    }
                });
                mqttService.sendRetainedMessage(Topico.DASHBOARD, "Atualizando dashboard todos");
            } else {
                logRepository.save(Log.builder()
                        .key(UUID.randomUUID())
                        .data(LocalDateTime.now())
                        .usuario("request.getUsuario()")
                        .cor(null)
                        .mensagem("Nenhum dos dispositos estão ativos")
                        .comando(Comando.NENHUM_DEVICE)
                        .descricao(Comando.NENHUM_DEVICE.value)
                        .id(0)
                        .build());
            }
            return "Comando enviado para todos";
        } catch (Exception erro) {
            logger.info("Erro ao sincronizar");
            logger.error(erro.getMessage());
            return "Sincronização não foi concluida";
        }
    }

    public static byte[] hexStringToByteArray(String hex) {
        int length = hex.length();
        byte[] data = new byte[length / 2]; // Cada 2 caracteres = 1 byte

        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }


    private Dispositivo buscarPorId(long id) {

        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(id);
        if (dispositivoOptional.isPresent()) {
            return dispositivoOptional.get();
        } else if (ComandoService.streams.containsKey(id)) {
            ComandoService.streams.remove(id).success(id + " não encontrado ou inativo ");
        }
        return null;
    }

    private List<Dispositivo> listaTodosDispositivos(TipoConfiguracao tipoConfiguracao, UUID cor) {
        if (tipoConfiguracao.equals(TipoConfiguracao.LED))
            return dispositivoRepository.findAllByAtivo(true);
        else if (tipoConfiguracao.equals(TipoConfiguracao.VIBRACAO) && cor != null)
            return dispositivoRepository.findAllByCorVibracao(cor.toString());
        return Collections.emptyList();
    }

}
