package com.led.broker.service;

import com.led.broker.model.*;
import com.led.broker.model.constantes.*;
import com.led.broker.repository.ConexaoRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.repository.OperacaoRepository;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
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
        }
    }

    public void atualizarDispositivo(Mensagem mensagem) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findByIdAndAtivo(mensagem.getId(), true);
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

            if (gerarLog || atualizarDashboard) {
                dashboardService.atualizarDashboard("", true);
                mqttService.sendRetainedMessage(Topico.TOPICO_DASHBOARD, "Atualizando dashboard");
            }
            if (gerarLog){
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("Enviado pelo dispositivo")
                        .mensagem(mensagem.getId())
                        .cor(dispositivo.getCor())
                        .comando(mensagem.getComando())
                        .descricao(mensagem.getComando().equals(Comando.ONLINE) ? String.format(mensagem.getComando().value(), mensagem.getId()) : mensagem.getComando().value())
                        .mac(dispositivo.getMac())
                        .build());
            }
            Cor cor = getCor(dispositivo);
            if (cor != null) {
                if (mensagem.getComando().equals(Comando.CONFIGURACAO) || mensagem.getComando().equals(Comando.CONCLUIDO)) {
                    dispositivo.setCor(cor);
                    System.out.println(mensagem.getComando().value());
                    comandoService.enviardComandoSincronizar(dispositivo.getMac(), false);
                } else if (mensagem.getComando().equals(Comando.ONLINE) && mensagem.getEfeito() != null) {
                    if (!cor.getEfeito().equals(mensagem.getEfeito())) {
                        System.out.println("Reparação de efeito de " + cor.getEfeito() + " para " + mensagem.getEfeito());
                        dispositivo.setCor(cor);
                        comandoService.enviardComandoSincronizar(dispositivo);
                    }
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

                conexaoRepository.save(dispositivo.getConexao());
                operacaoRepository.save(dispositivo.getOperacao());
                dashboardService.atualizarDashboard("", true);
                mqttService.sendRetainedMessage(Topico.TOPICO_DASHBOARD, "Atualizando dashboard");
            }
        }
    }

    private Cor getCor(Dispositivo dispositivo) {

        if (dispositivo.getOperacao().equals(ModoOperacao.DISPOSITIVO)) {
            return dispositivo.getCor();
        }

        if (dispositivo.getOperacao().equals(ModoOperacao.TEMPORIZADOR)) {
            if (TimeUtil.isTime(dispositivo)) {
                if (dispositivo.getOperacao().getCorTemporizador() != null) {
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
                if (isBetween)
                    return agenda.getCor();
            }
        }

        if (!dispositivo.getOperacao().equals(ModoOperacao.DISPOSITIVO)) {
            dispositivo.getOperacao().setModoOperacao(ModoOperacao.DISPOSITIVO);
            operacaoRepository.save(dispositivo.getOperacao());
        }

        return dispositivo.getCor();
    }

    public List<Conexao> dispositivosQueFicaramOffilne() {

        Date cincoMinutosAtras = Date.from(Instant.now().minusSeconds(5 * 60));
        Criteria criteria = Criteria.where("ultimaAtualizacao").lt(cincoMinutosAtras).and("status").is("Online");
        Query query = new Query(criteria);
        return mongoTemplate.find(query, Conexao.class);
    }
}
