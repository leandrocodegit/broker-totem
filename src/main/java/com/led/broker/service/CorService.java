package com.led.broker.service;

import com.led.broker.model.*;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.ModoOperacao;
import com.led.broker.model.constantes.TipoConexao;
import com.led.broker.model.constantes.Topico;
import com.led.broker.repository.*;
import com.led.broker.util.CorUtil;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CorService {

    private static final Logger logger = LoggerFactory.getLogger(CorService.class);
    private final CorRepository corRepository;
    private final DispositivoRepository dispositivoRepository;
    private final ComandoService comandoService;
    private final LogRepository logRepository;
    private final AgendaDeviceService agendaDeviceService;
    private final OperacaoRepository operacaoRepository;
    private final MqttService mqttService;
    private final CorUtil corUtil;
    private final UserRepository userRepository;

    public Cor buscaCor(UUID id) {
        return corRepository.findById(id).orElseThrow(() -> new RuntimeException("Cor inválida ou removida"));
    }


    public void cancelarComando(DispositivoEntity dispositivoEntity, String user) {
        logger.warn("Cancelando comando");
        if (dispositivoEntity.getOperacao().getModoOperacao().equals(ModoOperacao.TEMPORIZADOR)) {
            setOperacao(dispositivoEntity);
            operacaoRepository.save(dispositivoEntity.getOperacao());
            logRepository.save(Log.builder()
                    .key(UUID.randomUUID())
                    .cliente(dispositivoEntity.getCliente())
                    .data(LocalDateTime.now())
                    .usuario(user)
                    .mensagem(String.format(Comando.TIMER_CANCELADO.value, dispositivoEntity.getId()))
                    .cor(null)
                    .comando(Comando.TIMER_CANCELADO)
                    .descricao(String.format(Comando.TIMER_CANCELADO.value, dispositivoEntity.getId()))
                    .id(dispositivoEntity.getId())
                    .build());
            dispositivoRepository.save(dispositivoEntity);
            if (dispositivoEntity.getCliente() != null)
                mqttService.sendRetainedMessage(Topico.MAPA + "/" + dispositivoEntity.getCliente().getId().toString(), "Atualizar mapa");
            comandoService.enviardComandoRapido(dispositivoEntity, false, true, false);
        }else{
            logger.warn("Comando já foi cancelado");
        }
    }

    public Mono<String> salvarCorTemporizada(UUID idCor, long id, boolean responder, boolean cancelar, String user) {

        try {
            Optional<DispositivoEntity> dispositivoOptional = dispositivoRepository.findById(id);
            if (dispositivoOptional.isPresent() && dispositivoOptional.get().isPermiteComando()) {
                if (responder && dispositivoOptional.get().getConexao().getTipoConexao().equals(TipoConexao.LORA))
                    responder = false;
                if (cancelar) {
                    logger.warn("Cancelando comando");
                    DispositivoEntity dispositivoEntity = dispositivoOptional.get();
                    if (!dispositivoEntity.getOperacao().getModoOperacao().equals(ModoOperacao.TEMPORIZADOR)) {
                        logger.warn("Comando já foi cancelado");
                        return Mono.just("Comando já foi cancelado");
                    }
                    setOperacao(dispositivoEntity);
                    operacaoRepository.save(dispositivoEntity.getOperacao());
                    logRepository.save(Log.builder()
                            .key(UUID.randomUUID())
                            .cliente(dispositivoEntity.getCliente())
                            .data(LocalDateTime.now())
                            .usuario(user)
                            .mensagem(String.format(Comando.TIMER_CANCELADO.value, dispositivoEntity.getId()))
                            .cor(null)
                            .comando(Comando.TIMER_CANCELADO)
                            .descricao(String.format(Comando.TIMER_CANCELADO.value, dispositivoEntity.getId()))
                            .id(dispositivoEntity.getId())
                            .build());
                    dispositivoRepository.save(dispositivoEntity);
                    if (dispositivoEntity.getCliente() != null)
                        mqttService.sendRetainedMessage(Topico.MAPA + "/" + dispositivoEntity.getCliente().getId().toString(), "Atualizar mapa");
                    return comandoService.enviardComandoRapido(dispositivoEntity, responder, true, false);
                } else {
                    Optional<Cor> corOptional = corRepository.findById(idCor);
                    if (corOptional.isPresent()) {
                        DispositivoEntity dispositivoEntity = dispositivoOptional.get();

                        var modoOcorrencia = dispositivoEntity.getOperacao().getModoOperacao().equals(ModoOperacao.OCORRENCIA) || dispositivoEntity.getOperacao().getModoOperacao().equals(ModoOperacao.BOTAO);
                        // if (modoOcorrencia)

                        dispositivoEntity.getOperacao().setModoOperacao(ModoOperacao.TEMPORIZADOR);
                        dispositivoEntity.getOperacao().setTime(LocalDateTime.now().plusSeconds(corOptional.get().getTime()));
                        dispositivoEntity.getOperacao().setCorTemporizador(buscaCor(idCor));
                        operacaoRepository.save(dispositivoEntity.getOperacao());
                        dispositivoRepository.save(dispositivoEntity);
                        dispositivoEntity.setCor(corUtil.parametricarCorDispositivo(corOptional.get(), dispositivoEntity));
                        TimeUtil.timers.put(dispositivoEntity.getId(), TemporizadorControle.builder()
                                .timeout(LocalDateTime.now().plusSeconds(corOptional.get().getTime()))
                                .dispositivoEntity(dispositivoEntity).build());
                        logRepository.save(Log.builder()
                                .key(UUID.randomUUID())
                                .cliente(dispositivoEntity.getCliente())
                                .data(LocalDateTime.now())
                                .usuario(user)
                                .mensagem(String.format(Comando.TIMER_CRIADO.value, dispositivoEntity.getId()))
                                .cor(null)
                                .comando(Comando.TIMER_CRIADO)
                                .descricao(String.format(Comando.TIMER_CRIADO.value, dispositivoEntity.getId()))
                                .id(dispositivoEntity.getId())
                                .build());
                        logger.warn("Temporizador criado para " + dispositivoEntity.getId());
                        if (dispositivoEntity.getCliente() != null)
                            mqttService.sendRetainedMessage(Topico.MAPA + "/" + dispositivoEntity.getCliente().getId().toString(), "Atualizar mapa");
                        if (modoOcorrencia)
                            return Mono.just("Atualizado");
                        return comandoService.enviardComandoRapido(dispositivoEntity, responder, false, false);
                    } else {
                        logger.error("Falha, cor não existe ou não encontrada");
                        return Mono.just("Falha, cor não existe ou não encontrada");
                    }
                }
            }
        } catch (Exception errr) {
            logger.error(errr.getMessage());
            return Mono.just("Falha ao enviar comando");
        }
        return Mono.just("Dispositivo não pemite enviar comandos");
    }

    public void salvarCorTemporizadaReponse(UUID idCor, long id, boolean cancelar, boolean retentar, String user) {

        try {

            Optional<DispositivoEntity> dispositivoOptional = dispositivoRepository.findById(id);
            if (dispositivoOptional.isPresent() && dispositivoOptional.get().isPermiteComando()) {
                if (cancelar) {
                    DispositivoEntity dispositivoEntity = dispositivoOptional.get();
                    setOperacao(dispositivoEntity);

                    dispositivoRepository.save(dispositivoEntity);
                    comandoService.enviardComandoRapido(dispositivoEntity, false, true, true);
                    if (dispositivoEntity.getCliente() != null)
                        mqttService.sendRetainedMessage(Topico.MAPA + "/" + dispositivoEntity.getCliente().getId().toString(), "Atualizar mapa");
                    logRepository.save(Log.builder()
                            .key(UUID.randomUUID())
                            .cliente(dispositivoEntity.getCliente())
                            .data(LocalDateTime.now())
                            .usuario(user)
                            .mensagem(String.format(Comando.TIMER_CANCELADO.value, dispositivoEntity.getId()))
                            .cor(null)
                            .comando(Comando.TIMER_CANCELADO)
                            .descricao(String.format(Comando.TIMER_CANCELADO.value, dispositivoEntity.getId()))
                            .id(dispositivoEntity.getId())
                            .build());
                } else {
                    Optional<Cor> corOptional = corRepository.findById(idCor);
                    if (corOptional.isPresent()) {
                        DispositivoEntity dispositivoEntity = dispositivoOptional.get();

                        var modoOcorrencia = dispositivoEntity.getOperacao().equals(ModoOperacao.OCORRENCIA) || dispositivoEntity.getOperacao().equals(ModoOperacao.BOTAO);
                        if (!modoOcorrencia)
                            dispositivoEntity.getOperacao().setModoOperacao(ModoOperacao.TEMPORIZADOR);
                        dispositivoEntity.getOperacao().setTime(LocalDateTime.now());
                        dispositivoEntity.getOperacao().setCorTemporizador(buscaCor(idCor));
                        dispositivoRepository.save(dispositivoEntity);
                        dispositivoEntity.setCor(corOptional.get());
                        TimeUtil.timers.put(dispositivoEntity.getId(), TemporizadorControle.builder()
                                        .timeout(LocalDateTime.now().plusSeconds(corOptional.get().getTime()))
                                .dispositivoEntity(dispositivoEntity).build());
                        if (!modoOcorrencia)
                            comandoService.enviardComandoRapido(dispositivoEntity, false, false, true);
                        if (dispositivoEntity.getCliente() != null)
                            mqttService.sendRetainedMessage(Topico.MAPA + "/" + dispositivoEntity.getCliente().getId().toString(), "Atualizar mapa");
                        logRepository.save(Log.builder()
                                .key(UUID.randomUUID())
                                .cliente(dispositivoEntity.getCliente())
                                .data(LocalDateTime.now())
                                .usuario(user)
                                .mensagem(String.format(Comando.TIMER_CRIADO.value, dispositivoEntity.getId()))
                                .cor(null)
                                .comando(Comando.TIMER_CRIADO)
                                .descricao(String.format(Comando.TIMER_CRIADO.value, dispositivoEntity.getId()))
                                .id(dispositivoEntity.getId())
                                .build());
                    }
                }
            }
        } catch (Exception errr) {
            logger.error(errr.getMessage());
            if (retentar) {
                salvarCorTemporizadaReponse(idCor, id, false, false, user);
            } else {
                throw new RuntimeException("Erro ao enviar comando");
            }
        }
    }

    public void setOperacao(DispositivoEntity dispositivoEntity) {
        Agenda agenda = null;

        dispositivoEntity.getOperacao().setModoOperacao(ModoOperacao.DISPOSITIVO);

        if (Boolean.FALSE.equals(dispositivoEntity.isIgnorarAgenda())) {
            agenda = agendaDeviceService.buscarAgendaDipositivoPrevistaHoje(dispositivoEntity.getId());
            if (agenda == null) {
                List<Agenda> agendasParatodosHoje = agendaDeviceService.listaTodosAgendasPrevistaHoje(true);
                if (!agendasParatodosHoje.isEmpty()) {
                    agenda = agendasParatodosHoje.stream().findFirst().get();
                }
            }
            if (agenda != null && agenda.getCor() != null) {
                dispositivoEntity.getOperacao().setModoOperacao(ModoOperacao.AGENDA);
                dispositivoEntity.getOperacao().setAgenda(agenda);
            }
        }
    }
}
