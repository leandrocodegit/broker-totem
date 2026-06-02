package com.led.broker.service;


import com.led.broker.controller.request.DispositivoRequest;
import com.led.broker.integracao.model.Dispositivo;
import com.led.broker.integracao.rest.DispositivoRest;
import com.led.broker.model.*;
import com.led.broker.model.constantes.*;
import com.led.broker.repository.CorRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.util.ComandoFormater;
import com.led.broker.util.CorUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ComandoService {

    private static final Logger logger = LoggerFactory.getLogger(ComandoService.class);
    private final MqttService mqttService;
    private final DispositivoRest dispositivoRest;
    private final CorRepository corRepository;
    private final LogRepository logRepository;
    private final CorUtil corUtil;
    public static Map<UUID, MonoSink<String>> streams = new HashMap<>();
    public static Map<String, UUID> clientes = new HashMap<>();


    public Mono<String> createMono(UUID id) {
        return Mono.create(sink -> {
            streams.put(id, sink);
        });
    }

    public Mono<String> enviardComandoTeste(UUID id) {

        logger.warn("Comando de teste: " + id);
        Dispositivo dispositivoEntity = buscarPorId(id);

        Mono<String> mono = createMono(id);

//        if (dispositivo != null && dispositivo.getConfiguracao() != null) {
//            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivo.getMac(), ConfiguracaoUtil.gerarComandoTeste(dispositivo.getConfiguracao()));
//        }

        return mono;
    }

    public void enviardComandoSincronizarId(UUID id, int topico) {

        if (topico <= 1000) {
            var dispositivo = buscarPorId(id);
            if (dispositivo != null) {
                mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + topico, ComandoFormater.gerarConfiguracaoId(id));
            }
        }
    }

    public Mono<String> enviardComandoSincronizar(UUID id, boolean responder, TipoConfiguracao tipoConfiguracao) {
        return enviardComandoSincronizar(id, responder, tipoConfiguracao, false);
    }

    public void enviardComandoSincronizar(DispositivoRequest request) {
        var topico = Topico.DEVICE_RECEIVE + request.getId();
        mqttService.sendRetainedMessage(topico, ComandoFormater.gerarCodigoCor(request));
    }
        public Mono<String> enviardComandoSincronizar(UUID id, boolean responder, TipoConfiguracao tipoConfiguracao, boolean forcarVibracao) {
        Dispositivo dispositivo = buscarPorId(id);

        if (dispositivo != null) {
            logger.error(id + " não encontrado ou inativo ");
            return Mono.just(id + " não encontrado ou inativo ");
        }

        if (dispositivo.getConexao().getStatus().equals(StatusConexao.Offline)) {
            return Mono.just("Dispositivo " + id + " offline ");
        }

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


        if (dispositivo.isAtivo()) {
            if (tipoConfiguracao.equals(TipoConfiguracao.LED) || tipoConfiguracao.equals(TipoConfiguracao.LED_RESTART)) {
                dispositivo.setCor(corUtil.repararCor(dispositivo));
            }
            if (forcarVibracao && !isLora && tipoConfiguracao.equals(TipoConfiguracao.LED)) {
                mqttService.sendRetainedMessage(topico, ComandoFormater.gerarCodigo(dispositivo, responder, TipoConfiguracao.VIBRACAO), dispositivoEntity.getConexao());
            }
            if (dispositivoEntity.getCor() != null || (!tipoConfiguracao.equals(TipoConfiguracao.LED) && !tipoConfiguracao.equals(TipoConfiguracao.LED_RESTART))) {
                mqttService.sendRetainedMessage(topico, ComandoFormater.gerarCodigo(dispositivo, responder, tipoConfiguracao), dispositivoEntity.getConexao());
                if (!responder) {
                    return mono.just("ok");
                }
            } else {
                logger.error(id + " não possui configuração de cor");
                return mono.just("não possui configuração de cor");
            }

        }
        if (isLora) {
            streams.remove(dispositivoEntity.getId());
            return mono.just("Comando enviado via LoraWan");
        }
        return mono;
    }

    public Mono<String> enviardComandoUpdateFirmware(UUID id, String host) {
        Optional<DispositivoEntity> dispositivoOptional = dispositivoRepository.findById(id);


        if (!dispositivoOptional.isPresent()) {
            logger.error(id + " não encontrado ou inativo ");
            return Mono.just(id + " não encontrado ou inativo ");
        }

        DispositivoEntity dispositivoEntity = dispositivoOptional.get();

        var isLora = dispositivoEntity.getConexao().getTipoConexao().equals(TipoConexao.LORA);
        if (isLora) {
            streams.remove(dispositivoEntity.getId());
            return Mono.just("Opção não disponivel para conexão LoraWan");
        }
        Mono<String> mono = createMono(id);

           mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivoEntity.getId(), ComandoFormater.gerarCodigoFirmware(host + dispositivoEntity.getId()));
        return mono;
    }

    public Mono<String> enviardComandoRapido(DispositivoEntity dispositivoEntity, boolean responder, boolean cancelar, boolean interno) {

        Mono<String> mono = Mono.empty();

        if (!interno) {
            mono = createMono(dispositivoEntity.getId());
        }

        if (cancelar) {
            logger.warn("Cancelar comando rápido: " + dispositivoEntity.getId());
            dispositivoEntity.setCor(corUtil.repararCor(buscarPorId(dispositivoEntity.getId())));
        } else {
            dispositivoEntity.setCor(corUtil.parametricarCorDispositivo(dispositivoEntity.getCor(), dispositivoEntity));
        }

        if (responder && isLora)
            responder = false;

        if (dispositivoEntity.isAtivo() && dispositivoEntity.getCor() != null) {
            mqttService.sendRetainedMessage(Topico.DEVICE_RECEIVE + dispositivoEntity.getId(), ComandoFormater.gerarCodigoCor(dispositivoEntity, responder, TipoConfiguracao.LED), dispositivoEntity.getConexao());
        }

        logger.warn("Comando rápido criado: " + dispositivoEntity.getId());

        if (isLora) {
            streams.remove(dispositivoEntity.getId());
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
            List<DispositivoEntity> dispositivoEntities = listaTodosDispositivos(clienteId, tipoConfiguracao, cor);
            logger.warn("Comando enviado para todos: " + dispositivoEntities.size());

            if (!dispositivoEntities.isEmpty()) {
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

                dispositivoEntities.forEach(device -> {
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

    private Dispositivo buscarPorId(UUID id) {
        return dispositivoRest.buscarDispositivo(id);
    }

    private List<DispositivoEntity> listaTodosDispositivos(UUID clienteId, TipoConfiguracao tipoConfiguracao, UUID cor) {
//        if (tipoConfiguracao.equals(TipoConfiguracao.LED))
//            return dispositivoRepository.findAllByAtivo(clienteId, true);
//        else if (tipoConfiguracao.equals(TipoConfiguracao.VIBRACAO) && cor != null)
//            return dispositivoRepository.findAllByCorVibracao(cor.toString());
        return Collections.emptyList();
    }

}
