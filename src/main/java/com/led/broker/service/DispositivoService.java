package com.led.broker.service;

import com.led.broker.model.*;
import static com.led.broker.model.constantes.Comando.*;
import static com.led.broker.model.constantes.StatusConexao.*;
import static com.led.broker.model.constantes.Topico.*;
import static com.led.broker.model.constantes.ModoOperacao.*;
import static com.led.broker.model.constantes.TipoCor.*;
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
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

            conexoes.forEach(conexao -> conexao.setStatus(Offline));

            conexaoRepository.saveAll(conexoes);

            logRepository.save(Log.builder()
                    .data(LocalDateTime.now())
                    .usuario("Enviado pelo sistema")
                    .mensagem("Dispositivos offline")
                    .cor(null)
                    .comando(OFFLINE)
                    .descricao(String.format(OFFLINE.value(), "grupo"))
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
            boolean gerarLog = mensagem.getComando().equals(ONLINE) && dispositivo.getConexao().getStatus().equals(Offline);
            boolean atualizarDashboard = mensagem.getComando().equals(CONFIGURACAO) && dispositivo.getConexao().getStatus().equals(Offline);

            if (dispositivo.getConexao() == null) {
                dispositivo.setConexao(Conexao.builder()
                        .mac(dispositivo.getMac())
                        .build());
            }
            dispositivo.getConexao().setUltimaAtualizacao(LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime());
            dispositivo.getConexao().setStatus(Online);
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
                dashboardService.atualizarDashboard("");
                mqttService.sendRetainedMessage(TOPICO_DASHBOARD, "Atualizando dashboard");
            }
            if (gerarLog) {
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("Enviado pelo dispositivo")
                        .mensagem(mensagem.getId())
                        .cor(dispositivo.getCor())
                        .comando(mensagem.getComando())
                        .descricao(mensagem.getComando().equals(ONLINE) ? String.format(mensagem.getComando().value(), mensagem.getId()) : mensagem.getComando().value())
                        .mac(dispositivo.getMac())
                        .build());
                logger.warn("Criado log de tarefa");
            }
            sincronizar(dispositivo, mensagem);
        } else {
            if (dispositivoRepository.countByAtivo(true) < quantidadeClientes && dispositivoRepository.countByAtivo(false) < quantidadeClientes + 100) {
                Dispositivo dispositivo = dispositivoRepository.save(
                        Dispositivo.builder()
                                .conexao(Conexao.builder()
                                        .mac(mensagem.getId())
                                        .status(Online)
                                        .ultimaAtualizacao(LocalDateTime.now())
                                        .build())
                                .mac(mensagem.getId())
                                .versao(mensagem.getVersao())
                                .ignorarAgenda(false)
                                .operacao(Operacao.builder()
                                        .mac(mensagem.getId())
                                        .modoOperacao(DISPOSITIVO)
                                        .build())
                                .memoria(0)
                                .ativo(false)
                                .permiteComando(true)
                                .nome(mensagem.getId().substring(mensagem.getId().length() - 5, mensagem.getId().length()))
                                .comando(ONLINE)
                                .configuracao(new Configuracao(1, 255, 2, RBG))
                                .build());
                logger.warn("Novo dispositivo adicionado " + dispositivo.getMac());
                conexaoRepository.save(dispositivo.getConexao());
                operacaoRepository.save(dispositivo.getOperacao());
                dashboardService.atualizarDashboard("");
                mqttService.sendRetainedMessage(TOPICO_DASHBOARD, "Atualizando dashboard");
            }
        }
    }

    public void sincronizar(Mensagem mensagem){
        logger.warn("Executando sincronizar: " + mensagem.getId());
        mensagem.setComando(CONFIGURACAO);
        dispositivoRepository.findByIdAndAtivo(mensagem.getId(), true).ifPresent(device -> sincronizar(device, mensagem));
    }

    public void sincronizar(Dispositivo dispositivo, Mensagem mensagem){
        dispositivo.setCor(getCor(dispositivo));
        logger.info("Nova mensagem " + mensagem.getComando().value());
        if (Stream.of(CONCLUIDO, CONFIGURACAO).anyMatch(cmd -> cmd.equals(mensagem.getComando()))) {
            comandoService.enviardComandoSincronizar(dispositivo);
            logger.warn("Tarefa de configuração executada");
        } else if (mensagem.getComando().equals(ONLINE) && mensagem.getEfeito() != null) {
            if (!dispositivo.getCor().getEfeito().equals(mensagem.getEfeito())) {
                logger.warn("Reparação de efeito de " + dispositivo.getCor().getEfeito() + " para " + mensagem.getEfeito());
                comandoService.enviardComandoSincronizar(dispositivo);
            }
        }
    }

    private Cor getCor(Dispositivo dispositivo) {

        logger.warn("Recuperando cor");
        if (dispositivo.getOperacao().getModoOperacao().equals(DISPOSITIVO)) {
            logger.warn("Tipo: " + dispositivo.getOperacao().getModoOperacao());
            return dispositivo.getCor();
        }

        if (dispositivo.getOperacao().getModoOperacao().equals(TEMPORIZADOR) && dispositivo.isPermiteComando()) {
            if (TimeUtil.isTime(dispositivo)) {
                if (dispositivo.getOperacao().getCorTemporizador() != null) {
                    logger.warn("Tipo: " + dispositivo.getOperacao().getModoOperacao());
                    return dispositivo.getOperacao().getCorTemporizador();
                }
            }
        }

        if (Boolean.FALSE.equals(dispositivo.isIgnorarAgenda()) && dispositivo.getOperacao().getModoOperacao().equals(AGENDA)) {
            Agenda agenda = dispositivo.getOperacao().getAgenda();
            if (agenda != null && agenda.getCor() != null && agenda.isAtivo() && agenda.getDispositivos().contains(dispositivo.getMac())) {

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

        if (!dispositivo.getOperacao().getModoOperacao().equals(DISPOSITIVO)) {
            dispositivo.getOperacao().setModoOperacao(DISPOSITIVO);
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
