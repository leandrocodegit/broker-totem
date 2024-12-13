package com.led.broker.service;

import com.led.broker.model.*;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.ModoOperacao;
import com.led.broker.model.constantes.StatusConexao;
import com.led.broker.model.constantes.TipoCor;
import com.led.broker.repository.ConexaoRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.repository.OperacaoRepository;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.ZoneOffset;
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
    private final CorService configuracaoService;
    private final ComandoService comandoService;
    private final OperacaoRepository operacaoRepository;
    private final ConexaoRepository conexaoRepository;


    public void salvarDispositivoComoOffline(List<Conexao> conexoes) {
        if(conexoes != null && !conexoes.isEmpty()) {
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

            if(dispositivo.getConexao() == null){
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
            if (mensagem.getComando().equals(Comando.ONLINE) && dispositivo.getConexao().getStatus().equals(StatusConexao.Offline))
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("Enviado pelo dispositivo")
                        .mensagem(mensagem.getId())
                        .cor(dispositivo.getCor())
                        .comando(mensagem.getComando())
                        .descricao(mensagem.getComando().equals(Comando.ONLINE) ? String.format(mensagem.getComando().value(), mensagem.getId()) : mensagem.getComando().value())
                        .mac(dispositivo.getMac())
                        .build());

            dispositivoRepository.save(dispositivo);
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
           if(dispositivoRepository.countByAtivo(true) < quantidadeClientes && dispositivoRepository.countByAtivo(false) < quantidadeClientes + 100) {
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

               dispositivo.setConexao(Conexao.builder()
                       .mac(dispositivo.getMac())
                       .build());
               conexaoRepository.save(dispositivo.getConexao());
               operacaoRepository.save(dispositivo.getOperacao());
           }
        }
    }

    private Cor getCor(Dispositivo dispositivo) {

        if(dispositivo.getOperacao().equals(ModoOperacao.DISPOSITIVO)){
           return dispositivo.getCor();
        }

        if(dispositivo.getOperacao().equals(ModoOperacao.TEMPORIZADOR)){
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
                    if(isBetween)
                        return agenda.getCor();
            }
        }

        if(!dispositivo.getOperacao().equals(ModoOperacao.DISPOSITIVO)){
            dispositivo.getOperacao().setModoOperacao(ModoOperacao.DISPOSITIVO);
            operacaoRepository.save(dispositivo.getOperacao());
        }

        return dispositivo.getCor();
    }

    public List<Conexao> dispositivosQueFicaramOffilne() {
        LocalDateTime cincoMinutosAtras = LocalDateTime.now(ZoneOffset.UTC).plusHours(3).minusMinutes(6);
        Date dataLimite = Date.from(cincoMinutosAtras.atZone(ZoneOffset.UTC).toInstant());
        return dispositivoRepository.findAllAtivosComUltimaAtualizacaoAntesQueEstavaoOnline(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(6)).stream().map(device -> device.getConexao()).toList();
    }
}
