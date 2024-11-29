package com.led.broker.service;

import com.led.broker.model.*;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.Efeito;
import com.led.broker.model.constantes.StatusConexao;
import com.led.broker.model.constantes.TipoCor;
import com.led.broker.repository.ConexaoRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DispositivoService {

    private final DispositivoRepository dispositivoRepository;
    private final LogRepository logRepository;
    private final CorService configuracaoService;
    private final ComandoService comandoService;
    private final AgendaDeviceService agendaDeviceService;
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
    @Async("taskExecutor")
    public void atualizarDispositivo(Mensagem mensagem) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mensagem.getId());
        if (dispositivoOptional.isPresent()) {
            Dispositivo dispositivo = dispositivoOptional.get();

            if(dispositivo.getConexao() == null){
                dispositivo.setConexao(Conexao.builder()
                                .mac(dispositivo.getMac())
                        .build());
            }

            dispositivo.getConexao().setUltimaAtualizacao(LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime());
            dispositivo.getConexao().setStatus(StatusConexao.Online);
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
            dispositivoRepository.save(
                    Dispositivo.builder()
                            .conexao(Conexao.builder()
                                    .ultimaAtualizacao(LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime())
                                    .status(StatusConexao.Online)
                                    .build())
                            .mac(mensagem.getId())
                            .versao("")
                            .ignorarAgenda(false)
                            .memoria(0)
                            .ativo(false)
                            .nome(mensagem.getId().substring(mensagem.getId().length() - 5, mensagem.getId().length()))
                            .comando(Comando.ONLINE)
                            .configuracao(new Configuracao(1, 255, 2, TipoCor.RBG))
                            .build()
            );
        }
    }

    private Cor getCor(Dispositivo dispositivo) {
        Agenda agenda = null;

        if (TimeUtil.isTime(dispositivo)) {
            Optional<Cor> corOptional = configuracaoService.buscaCor(dispositivo.getTemporizador().getIdCor());
            if (corOptional.isPresent()) {
                return corOptional.get();
            }
        }
        if (Boolean.FALSE.equals(dispositivo.isIgnorarAgenda())) {
            agenda = agendaDeviceService.buscarAgendaDipositivoPrevistaHoje(dispositivo.getMac());
            if(agenda == null){
                List<Agenda> agendasParatodosHoje = agendaDeviceService.listaTodosAgendasPrevistaHoje();
                if(!agendasParatodosHoje.isEmpty()){
                    agenda = agendasParatodosHoje.stream().findFirst().get();
                }
            }
        }
        if (agenda != null && agenda.getCor() != null) {
            return agenda.getCor();
        }
        return dispositivo.getCor();
    }

    public List<Conexao> dispositivosQueFicaramOffilne() {
        LocalDateTime cincoMinutosAtras = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(6);
        Date dataLimite = Date.from(cincoMinutosAtras.atZone(ZoneOffset.UTC).toInstant());
        return conexaoRepository.findAllAtivosComUltimaAtualizacaoAntesQueEstavaoOnline(dataLimite);
    }
}
