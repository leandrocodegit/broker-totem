package com.led.broker.service;

import com.led.broker.model.*;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.StatusConexao;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public void salvarDispositivoComoOffline(Dispositivo dispositivo) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(dispositivo.getMac());
        if (dispositivoOptional.isPresent()) {
            Dispositivo dispositivoDB = dispositivoOptional.get();
            dispositivoDB.setStatus(StatusConexao.Offline);
            dispositivoRepository.save(dispositivo);
        }
    }


    public void atualizarDispositivo(Mensagem mensagem) {
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mensagem.getId());
        if (dispositivoOptional.isPresent()) {
            Dispositivo dispositivo = dispositivoOptional.get();

            dispositivo.setUltimaAtualizacao(LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime());
            dispositivo.setIp(mensagem.getIp());
            dispositivo.setMemoria(mensagem.getMemoria());
            dispositivo.setComando(Comando.ONLINE);
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
            if (dispositivo.getConfiguracao() != null && (mensagem.getComando().equals(Comando.CONFIGURACAO) || mensagem.getComando().equals(Comando.CONCLUIDO))) {
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("Enviado pelo dipositivo")
                        .mensagem(mensagem.getId())
                        .cor(dispositivo.getCor())
                        .comando(mensagem.getComando())
                        .descricao(mensagem.getComando().equals(Comando.ONLINE) ? String.format(mensagem.getComando().value(), mensagem.getId()) : mensagem.getComando().value())
                        .mac(dispositivo.getMac())
                        .build());
            }
            if (mensagem.getComando().equals(Comando.ACEITO) || mensagem.getComando().equals(Comando.ONLINE)) {
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

            dispositivoRepository.save(dispositivo);
            Cor cor = getCor(dispositivo);
            if (cor != null) {
                if (mensagem.getComando().equals(Comando.CONFIGURACAO) || mensagem.getComando().equals(Comando.CONCLUIDO)) {
                    dispositivo.setCor(cor);
                    System.out.println("configuração");
                    comandoService.enviardComandoSincronizar(dispositivo.getMac(), false);
                } else if (mensagem.getComando().equals(Comando.ONLINE) && mensagem.getEfeito() != null) {
                    if (!cor.getEfeito().equals(mensagem.getEfeito())) {
                        System.out.println("Reparação de efeito");
                        dispositivo.setCor(cor);
                        comandoService.enviardComandoSincronizar(dispositivo.getMac(), false);
                    }
                }
            }
        } else {
            dispositivoRepository.save(
                    Dispositivo.builder()
                            .ultimaAtualizacao(LocalDateTime.now().atZone(ZoneOffset.UTC).toLocalDateTime())
                            .mac(mensagem.getId())
                            .versao("")
                            .ignorarAgenda(false)
                            .memoria(0)
                            .ativo(false)
                            .nome(mensagem.getId().substring(mensagem.getId().length() - 5, mensagem.getId().length()))
                            .comando(Comando.ONLINE)
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
        }
        if (agenda != null && agenda.getCor() != null) {
            return agenda.getCor();
        }
        return dispositivo.getCor();
    }

    public List<Dispositivo> buscarDispositivosAtivosTempo(long minutos) {
        LocalDateTime cincoMinutosAtras = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(minutos);
        Date dataLimite = Date.from(cincoMinutosAtras.atZone(ZoneOffset.UTC).toInstant());
        return dispositivoRepository.findAllAtivosComUltimaAtualizacaoAntes(dataLimite);
    }

    public Page<Dispositivo> buscarDispositivosAtivosTempo(long minutos, Pageable pageable) {
        LocalDateTime cincoMinutosAtras = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(minutos);
        Date dataLimite = Date.from(cincoMinutosAtras.atZone(ZoneOffset.UTC).toInstant());
        return dispositivoRepository.findAllAtivosComUltimaAtualizacaoAntes(dataLimite, pageable);
    }

    public List<Dispositivo> buscarDispositivosAtivosComAgendaPesquisada() {
        List<Dispositivo> dispositivos = dispositivoRepository.findAllByAtivo(true);
        if (!dispositivos.isEmpty()) {
            dispositivos.forEach(device -> {
                Agenda agenda = agendaDeviceService.buscarAgendaDipositivoPrevistaHoje(device.getMac());
                if (agenda != null && agenda.getCor() != null) {
                    device.setCor(agenda.getCor());
                }
            });
        }
        return dispositivos;
    }

    public List<String> listaTodosDispositivos() {
       return dispositivoRepository.findAllByAtivo(true).stream().map(device -> device.getMac()).toList();
    }

    public List<Dispositivo> dispositivosQueFicaramOffilne() {
        LocalDateTime cincoMinutosAtras = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(6);
        Date dataLimite = Date.from(cincoMinutosAtras.atZone(ZoneOffset.UTC).toInstant());
        return dispositivoRepository.findAllAtivosComUltimaAtualizacaoAntesQueEstavaoOnline(dataLimite);
    }
}
