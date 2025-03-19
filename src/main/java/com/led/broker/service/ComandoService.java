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
import java.util.stream.Stream;

import static com.led.broker.model.constantes.Comando.ACEITO;

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
    public static Map<String, UUID> clientes = new HashMap<>();


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
        return enviardComandoSincronizar(id, responder, tipoConfiguracao, false);
    }

    public Mono<String> enviardComandoSincronizar(long id, boolean responder, TipoConfiguracao tipoConfiguracao, boolean forcarVibracao) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(id);

        if (!dispositivoOptional.isPresent()) {
            logger.error(id + " não encontrado ou inativo ");
            return Mono.just(id + " não encontrado ou inativo ");
        }

        if (dispositivoOptional.get().getConexao().getStatus().equals(StatusConexao.Offline) && !dispositivoOptional.get().getConexao().getTipoConexao().equals(TipoConexao.LORA)) {
            return Mono.just("Dispositivo " + id + " offline ");
        }

        Dispositivo dispositivo = dispositivoOptional.get();
        var topico = Topico.DEVICE_RECEIVE + dispositivo.getId();
        var isLora = dispositivo.getConexao().getTipoConexao().equals(TipoConexao.LORA);
        if (dispositivo.getConexao().getTipoConexao().equals(TipoConexao.LORA))
            topico = Topico.KORE;

        Mono<String> mono = createMono(id);

        if (tipoConfiguracao.equals(TipoConfiguracao.LIMPAR_FLASH)) {
            mqttService.sendRetainedMessage(topico, ComandoFormater.gerarCodigoErase(dispositivo), dispositivo.getConexao());
            if (isLora) {
                streams.remove(dispositivo.getId());
                return mono.just("");
            }
            return mono;
        }
        if (tipoConfiguracao.equals(TipoConfiguracao.WIFI)) {
            if (dispositivo.getConexao().getSsid() == null || dispositivo.getConexao().getSsid().isEmpty())
                return Mono.just("Erro, SSID é obrigatório");
            mqttService.sendRetainedMessage(topico, ComandoFormater.gerarCodigoWIFI(dispositivo), dispositivo.getConexao());
            if (isLora) {
                streams.remove(dispositivo.getId());
                return mono.just("");
            }
            return mono;
        }
        if (Stream.of(TipoConfiguracao.LORA_WAN, TipoConfiguracao.LORA_WAN_PARAM, TipoConfiguracao.LORA_WAN_JOIN, TipoConfiguracao.LORA_WAN_SEND, TipoConfiguracao.LORA_WAN_RESET).anyMatch(tipo -> tipo.equals(tipoConfiguracao))) {
            if (dispositivo.getConexao().getHabilitarLoraWan() == null || dispositivo.getConexao().getHabilitarLoraWan() == Boolean.FALSE)
                return Mono.just("Erro, LoraWan não está habilitado");
            if (tipoConfiguracao.equals(TipoConfiguracao.LORA_WAN) && dispositivo.getConexao().getClasse() == null)
                return Mono.just("Erro, Classe LoraWan é obrigatória");
            mqttService.sendRetainedMessage(topico, ComandoFormater.gerarCodigoLora(dispositivo, true, tipoConfiguracao), dispositivo.getConexao());
            if (isLora) {
                streams.remove(dispositivo.getId());
                return mono.just("");
            }
            return mono;
        }

        if (dispositivo.isAtivo()) {
            if (tipoConfiguracao.equals(TipoConfiguracao.LED) || tipoConfiguracao.equals(TipoConfiguracao.LED_RESTART)) {
                dispositivo.setCor(corUtil.repararCor(dispositivo));
            }
            if (forcarVibracao && !isLora && tipoConfiguracao.equals(TipoConfiguracao.LED)) {
                mqttService.sendRetainedMessage(topico, ComandoFormater.gerarCodigo(dispositivo, responder, TipoConfiguracao.VIBRACAO), dispositivo.getConexao());
            }
            if (dispositivo.getCor() != null || (!tipoConfiguracao.equals(TipoConfiguracao.LED) && !tipoConfiguracao.equals(TipoConfiguracao.LED_RESTART))) {
                mqttService.sendRetainedMessage(topico, ComandoFormater.gerarCodigo(dispositivo, responder, tipoConfiguracao), dispositivo.getConexao());
                if (!responder) {
                    return mono.just("ok");
                }
            } else {
                logger.error(id + " não possui configuração de cor");
                return mono.just("não possui configuração de cor");
            }

        }
        if (isLora) {
            streams.remove(dispositivo.getId());
            return mono.just("");
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

        var isLora = dispositivo.getConexao().getTipoConexao().equals(TipoConexao.LORA);
        if (isLora) {
            streams.remove(dispositivo.getId());
            return Mono.just("Opção não disponivel para conexão LoraWan");
        }
        Mono<String> mono = createMono(id);

        //   mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), "ConfiguracaoUtil.gerarComandoFirmware(host)");
        return mono;
    }

    public Mono<String> enviardComandoRapido(Dispositivo dispositivo, boolean responder, boolean cancelar, boolean interno) {

        var isLora = dispositivo.getConexao().getTipoConexao().equals(TipoConexao.LORA);
        if (isLora) {
            streams.remove(dispositivo.getId());
            //  return Mono.just("Opção não disponivel para conexão LoraWan");
        }

        Mono<String> mono = Mono.empty();

        if (!interno) {
            mono = createMono(dispositivo.getId());
        }

        if (cancelar) {
            logger.warn("Cancelar comando rápido: " + dispositivo.getId());
            if (!dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.TEMPORIZADOR))
                return Mono.just("Comando já foi cancelado");
            dispositivo.setCor(corUtil.repararCor(buscarPorId(dispositivo.getId())));
        } else {
            dispositivo.setCor(corUtil.parametricarCorDispositivo(dispositivo.getCor(), dispositivo));
        }

        if (responder && isLora)
            responder = false;

        if (dispositivo.isAtivo() && dispositivo.getCor() != null) {
            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getId(), ComandoFormater.gerarCodigoCor(dispositivo, responder, TipoConfiguracao.LED), dispositivo.getConexao());
        }

        logger.warn("Comando rápido criado: " + dispositivo.getId());

        if (isLora) {
            streams.remove(dispositivo.getId());
            return Mono.just("Opção de resposta não disponivel para LoraWan");
        }
        return mono;
    }

    public String enviarComandoTodosVibracao(UUID clienteId, UUID cor) {
        enviarComandoTodos(clienteId, false, "Sistema", cor, TipoConfiguracao.VIBRACAO, true);
        logger.warn("Sincronizando cor de vibração " + cor.toString());
        return "Sincrolização enviada";
    }

    public String enviarComandoTodos(UUID clienteId, boolean responder, String user, UUID cor, TipoConfiguracao tipoConfiguracao, boolean interno) {

        try {
            List<Dispositivo> dispositivos = listaTodosDispositivos(clienteId, tipoConfiguracao, cor);
            logger.warn("Comando enviado para todos: " + dispositivos.size());

            if (!dispositivos.isEmpty()) {
                if (!interno)
                    logRepository.save(Log.builder()
                            .cliente(Cliente.builder().id(clienteId).principal(false).build())
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
                        if (device.getCliente() != null)
                            clientes.put(device.getCliente().getId().toString(), device.getCliente().getId());
                        mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + device.getId(), ComandoFormater.gerarCodigo(device, responder, tipoConfiguracao), device.getConexao());
                    }
                });
            } else {
                logRepository.save(Log.builder()
                        .key(UUID.randomUUID())
                        .cliente(Cliente.builder().id(clienteId).principal(false).build())
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

    private Dispositivo buscarPorId(long id) {

        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(id);
        if (dispositivoOptional.isPresent()) {
            return dispositivoOptional.get();
        } else if (ComandoService.streams.containsKey(id)) {
            ComandoService.streams.remove(id).success(id + " não encontrado ou inativo ");
        }
        return null;
    }

    private List<Dispositivo> listaTodosDispositivos(UUID clienteId, TipoConfiguracao tipoConfiguracao, UUID cor) {
        if (tipoConfiguracao.equals(TipoConfiguracao.LED))
            return dispositivoRepository.findAllByAtivo(clienteId, true);
        else if (tipoConfiguracao.equals(TipoConfiguracao.VIBRACAO) && cor != null)
            return dispositivoRepository.findAllByCorVibracao(cor.toString());
        return Collections.emptyList();
    }

}
