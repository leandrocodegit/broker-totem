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

    public Mono<String> salvarCorTemporizada(UUID idCor, long id, boolean responder, boolean cancelar, String user) {

        try {
            Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(id);
            if (dispositivoOptional.isPresent() && dispositivoOptional.get().isPermiteComando()) {
                if(responder && dispositivoOptional.get().getConexao().getTipoConexao().equals(TipoConexao.LORA))
                    responder = false;
                if (cancelar) {
                    logger.warn("Cancelando comando");
                    Dispositivo dispositivo = dispositivoOptional.get();
                    if(!dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.TEMPORIZADOR))
                        return Mono.just("Comando já foi cancelado");
                    setOperacao(dispositivo);
                    operacaoRepository.save(dispositivo.getOperacao());
                    logRepository.save(Log.builder()
                            .key(UUID.randomUUID())
                                    .cliente(dispositivo.getCliente())
                            .data(LocalDateTime.now())
                            .usuario(user)
                            .mensagem(String.format(Comando.TIMER_CANCELADO.value, dispositivo.getId()))
                            .cor(null)
                            .comando(Comando.TIMER_CANCELADO)
                            .descricao(String.format(Comando.TIMER_CANCELADO.value, dispositivo.getId()))
                            .id(dispositivo.getId())
                            .build());
                    dispositivoRepository.save(dispositivo);
                    if (dispositivo.getCliente() != null)
                        mqttService.sendRetainedMessage(Topico.MAPA + "/" + dispositivo.getCliente().getId().toString(), "Atualizar mapa");
                    return comandoService.enviardComandoRapido(dispositivo, responder,true, false);
                } else {
                    Optional<Cor> corOptional = corRepository.findById(idCor);
                    if (corOptional.isPresent()) {
                        Dispositivo dispositivo = dispositivoOptional.get();

                        var modoOcorrencia = dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.OCORRENCIA) || dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.BOTAO);
                        if (modoOcorrencia)
                            dispositivo.getOperacao().setModoOperacao(ModoOperacao.TEMPORIZADOR);
                        dispositivo.getOperacao().setTime(LocalDateTime.now().plusMinutes(corOptional.get().getTime()));
                        dispositivo.getOperacao().setCorTemporizador(buscaCor(idCor));
                        operacaoRepository.save(dispositivo.getOperacao());
                        dispositivoRepository.save(dispositivo);
                        dispositivo.setCor(corUtil.parametricarCorDispositivo(corOptional.get(), dispositivo));
                        TimeUtil.timers.put(dispositivo.getId(), dispositivo);
                        logRepository.save(Log.builder()
                                .key(UUID.randomUUID())
                                .cliente(dispositivo.getCliente())
                                .data(LocalDateTime.now())
                                .usuario(user)
                                .mensagem(String.format(Comando.TIMER_CRIADO.value, dispositivo.getId()))
                                .cor(null)
                                .comando(Comando.TIMER_CRIADO)
                                .descricao(String.format(Comando.TIMER_CRIADO.value, dispositivo.getId()))
                                .id(dispositivo.getId())
                                .build());
                        logger.warn("Temporizador criado para " + dispositivo.getId());
                        if (dispositivo.getCliente() != null)
                            mqttService.sendRetainedMessage(Topico.MAPA + "/" + dispositivo.getCliente().getId().toString(), "Atualizar mapa");
                        if (modoOcorrencia)
                            return Mono.just("Atualizado");
                        return comandoService.enviardComandoRapido(dispositivo, responder,false, false);
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

            Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(id);
            if (dispositivoOptional.isPresent() && dispositivoOptional.get().isPermiteComando()) {
                if (cancelar) {
                    Dispositivo dispositivo = dispositivoOptional.get();
                    setOperacao(dispositivo);

                    dispositivoRepository.save(dispositivo);
                    comandoService.enviardComandoRapido(dispositivo, false,true, true);
                    if (dispositivo.getCliente() != null)
                        mqttService.sendRetainedMessage(Topico.MAPA + "/" + dispositivo.getCliente().getId().toString(), "Atualizar mapa");
                    logRepository.save(Log.builder()
                            .key(UUID.randomUUID())
                            .cliente(dispositivo.getCliente())
                            .data(LocalDateTime.now())
                            .usuario(user)
                            .mensagem(String.format(Comando.TIMER_CANCELADO.value, dispositivo.getId()))
                            .cor(null)
                            .comando(Comando.TIMER_CANCELADO)
                            .descricao(String.format(Comando.TIMER_CANCELADO.value, dispositivo.getId()))
                            .id(dispositivo.getId())
                            .build());
                } else {
                    Optional<Cor> corOptional = corRepository.findById(idCor);
                    if (corOptional.isPresent()) {
                        Dispositivo dispositivo = dispositivoOptional.get();

                        var modoOcorrencia = dispositivo.getOperacao().equals(ModoOperacao.OCORRENCIA) || dispositivo.getOperacao().equals(ModoOperacao.BOTAO);
                        if (!modoOcorrencia)
                            dispositivo.getOperacao().setModoOperacao(ModoOperacao.TEMPORIZADOR);
                        dispositivo.getOperacao().setTime(LocalDateTime.now().plusMinutes(-1));
                        dispositivo.getOperacao().setCorTemporizador(buscaCor(idCor));
                        dispositivoRepository.save(dispositivo);
                        dispositivo.setCor(corOptional.get());
                        TimeUtil.timers.put(dispositivo.getId(), dispositivo);
                        if (!modoOcorrencia)
                            comandoService.enviardComandoRapido(dispositivo, false,false, true);
                        if (dispositivo.getCliente() != null)
                            mqttService.sendRetainedMessage(Topico.MAPA + "/" + dispositivo.getCliente().getId().toString(), "Atualizar mapa");
                        logRepository.save(Log.builder()
                                .key(UUID.randomUUID())
                                .cliente(dispositivo.getCliente())
                                .data(LocalDateTime.now())
                                .usuario(user)
                                .mensagem(String.format(Comando.TIMER_CRIADO.value, dispositivo.getId()))
                                .cor(null)
                                .comando(Comando.TIMER_CRIADO)
                                .descricao(String.format(Comando.TIMER_CRIADO.value, dispositivo.getId()))
                                .id(dispositivo.getId())
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

    public void setOperacao(Dispositivo dispositivo) {
        Agenda agenda = null;

        dispositivo.getOperacao().setModoOperacao(ModoOperacao.DISPOSITIVO);

        if (Boolean.FALSE.equals(dispositivo.isIgnorarAgenda())) {
            agenda = agendaDeviceService.buscarAgendaDipositivoPrevistaHoje(dispositivo.getId());
            if (agenda == null) {
                List<Agenda> agendasParatodosHoje = agendaDeviceService.listaTodosAgendasPrevistaHoje(true);
                if (!agendasParatodosHoje.isEmpty()) {
                    agenda = agendasParatodosHoje.stream().findFirst().get();
                }
            }
            if (agenda != null && agenda.getCor() != null) {
                dispositivo.getOperacao().setModoOperacao(ModoOperacao.AGENDA);
                dispositivo.getOperacao().setAgenda(agenda);
            }
        }
    }
}
