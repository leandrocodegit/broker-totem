package com.led.broker.service;

import com.led.broker.handler.MqttMessageHandler;
import com.led.broker.model.*;
import com.led.broker.model.constantes.*;
import com.led.broker.repository.ConexaoRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.repository.OperacaoRepository;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DispositivoService {

    private static final Logger logger = LoggerFactory.getLogger(DispositivoService.class);
    @Value("${quantidade-clientes}")
    private int quantidadeClientes;
    private final DispositivoRepository dispositivoRepository;
    private final LogRepository logRepository;
    private final DashboardService dashboardService;
    private final ComandoService comandoService;
    private final OperacaoRepository operacaoRepository;
    private final ConexaoRepository conexaoRepository;
    private final MongoTemplate mongoTemplate;
    private final MqttService mqttService;


    public void salvarDispositivoComoOffline(List<Conexao> conexoes) {
        if (conexoes != null && !conexoes.isEmpty()) {

            conexoes.forEach(conexao -> conexao.setStatus(StatusConexao.Offline));

            conexaoRepository.saveAll(conexoes);

            logRepository.save(Log.builder()
                    .data(LocalDateTime.now())
                    .usuario("Enviado pelo sistema")
                    .mensagem("Dispositivos offline")
                    .cor(null)
                    .comando(Comando.OFFLINE)
                    .descricao(String.format(Comando.OFFLINE.value(), "grupo"))
                    .mac(conexoes.stream().map(device -> device.getMac()).toList().toString())
                    .build());
            logger.warn("Erro ao capturar id");
        }
    }

    public void atualizarDispositivo(Mensagem mensagem) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findByIdAndAtivo(mensagem.getId(), true);
        logger.warn("Comando recebido: " + mensagem.getComando());
        if (dispositivoOptional.isPresent()) {

            Dispositivo dispositivo = dispositivoOptional.get();
            boolean gerarLog = mensagem.getComando().equals(Comando.ONLINE) && dispositivo.getConexao().getStatus().equals(StatusConexao.Offline);
            boolean atualizarDashboard = mensagem.getComando().equals(Comando.CONFIGURACAO) && dispositivo.getConexao().getStatus().equals(StatusConexao.Offline);

            if (dispositivo.getConexao() == null) {
                dispositivo.setConexao(Conexao.builder()
                        .mac(dispositivo.getMac())
                        .build());
            }
            dispositivo.getConexao().setUltimaAtualizacao(LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime());
            dispositivo.getConexao().setStatus(StatusConexao.Online);
            conexaoRepository.save(dispositivo.getConexao());
            logger.warn("Atualizado conexão:  " + dispositivo.getMac() + " : " + dispositivo.getConexao().getStatus());
            dispositivo.setIp(mensagem.getIp());
            dispositivo.setMemoria(mensagem.getMemoria());
            dispositivo.setComando(mensagem.getComando());
            dispositivo.setVersao(mensagem.getVersao());
            dispositivo.setBrokerId(mensagem.getBrockerId());

            if (dispositivo.getConfiguracao() == null) {
                dispositivo.setConfiguracao(Configuracao.builder()
                        .leds(1)
                        .intensidade(255)
                        .faixa(2)
                        .build()
                );
            }
            dispositivoRepository.save(dispositivo);
            logger.warn("Atualizado dispositivo:  " + dispositivo.getMac());

            if (gerarLog || atualizarDashboard) {
                dashboardService.atualizarDashboard("", true);
                mqttService.sendRetainedMessage(Topico.TOPICO_DASHBOARD, "Atualizando dashboard");
            }
            if (gerarLog) {
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("Enviado pelo dispositivo")
                        .mensagem(mensagem.getId())
                        .cor(dispositivo.getCor())
                        .comando(mensagem.getComando())
                        .descricao(mensagem.getComando().equals(Comando.ONLINE) ? String.format(mensagem.getComando().value(), mensagem.getId()) : mensagem.getComando().value())
                        .mac(dispositivo.getMac())
                        .build());
                logger.warn("Criado log de tarefa");
            }
            dispositivo.setCor(getCor(dispositivo));
            if (mensagem.getComando().equals(Comando.CONFIGURACAO) || mensagem.getComando().equals(Comando.CONCLUIDO)) {
                comandoService.enviardComandoSincronizar(dispositivo);
                logger.warn("Tarefa de configuração executada");
            } else if (mensagem.getComando().equals(Comando.ONLINE) && mensagem.getEfeito() != null) {
                if (!dispositivo.getCor().getEfeito().equals(mensagem.getEfeito())) {
                    logger.warn("Reparação de efeito de " + dispositivo.getCor().getEfeito() + " para " + mensagem.getEfeito());
                    comandoService.enviardComandoSincronizar(dispositivo);
                }
            }
        } else {
            if (dispositivoRepository.countByAtivo(true) < quantidadeClientes && dispositivoRepository.countByAtivo(false) < quantidadeClientes + 100) {
                Dispositivo dispositivo = dispositivoRepository.save(
                        Dispositivo.builder()
                                .conexao(Conexao.builder()
                                        .mac(mensagem.getId())
                                        .status(StatusConexao.Online)
                                        .ultimaAtualizacao(LocalDateTime.now())
                                        .build())
                                .mac(mensagem.getId())
                                .versao(mensagem.getVersao())
                                .ignorarAgenda(false)
                                .operacao(Operacao.builder()
                                        .mac(mensagem.getId())
                                        .modoOperacao(ModoOperacao.DISPOSITIVO)
                                        .build())
                                .memoria(0)
                                .ativo(false)
                                .nome(mensagem.getId().substring(mensagem.getId().length() - 5, mensagem.getId().length()))
                                .comando(Comando.ONLINE)
                                .configuracao(new Configuracao(1, 255, 2, TipoCor.RBG))
                                .build());
                logger.warn("Novo dispositivo adicionado " + dispositivo.getMac());
                conexaoRepository.save(dispositivo.getConexao());
                operacaoRepository.save(dispositivo.getOperacao());
                dashboardService.atualizarDashboard("", true);
                mqttService.sendRetainedMessage(Topico.TOPICO_DASHBOARD, "Atualizando dashboard");
            }
        }
    }

    private Cor getCor(Dispositivo dispositivo) {

        logger.warn("Recuperando cor");
        if (dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.DISPOSITIVO)) {
            logger.warn("Tipo: " + dispositivo.getOperacao().getModoOperacao());
            return dispositivo.getCor();
        }

        if (dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.TEMPORIZADOR)) {
            if (TimeUtil.isTime(dispositivo)) {
                if (dispositivo.getOperacao().getCorTemporizador() != null) {
                    logger.warn("Tipo: " + dispositivo.getOperacao().getModoOperacao());
                    return dispositivo.getOperacao().getCorTemporizador();
                }
            }
        }

        if (Boolean.FALSE.equals(dispositivo.isIgnorarAgenda()) && dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.AGENDA)) {
            Agenda agenda = dispositivo.getOperacao().getAgenda();
            if (agenda != null && agenda.getCor() != null) {

                MonthDay inicio = MonthDay.from(agenda.getInicio()); // 1º de novembro
                MonthDay fim = MonthDay.from(agenda.getTermino());  // 30 de novembro

                MonthDay hoje = MonthDay.from(LocalDate.now());

                boolean isBetween = false;
                if (inicio.isBefore(fim) || inicio.equals(fim)) {
                    isBetween = (hoje.equals(inicio) || hoje.isAfter(inicio)) &&
                            (hoje.equals(fim) || hoje.isBefore(fim));
                }
                if (isBetween) {
                    logger.warn("Tipo: " + dispositivo.getOperacao().getModoOperacao());
                    return agenda.getCor();
                }

            }
        }

        if (!dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.DISPOSITIVO)) {
            dispositivo.getOperacao().setModoOperacao(ModoOperacao.DISPOSITIVO);
            operacaoRepository.save(dispositivo.getOperacao());
            logger.warn("Rest modo operação: " + dispositivo.getOperacao().getModoOperacao());
        }

        logger.warn("Tipo: " + dispositivo.getOperacao().getModoOperacao());
        return dispositivo.getCor();
    }

    public List<Conexao> dispositivosQueFicaramOffilne() {

        Date cincoMinutosAtras = Date.from(Instant.now().minusSeconds(5 * 60));
        Criteria criteria = Criteria.where("ultimaAtualizacao").lt(cincoMinutosAtras).and("status").is("Online");
        Query query = new Query(criteria);
        return mongoTemplate.find(query, Conexao.class);
    }
}
