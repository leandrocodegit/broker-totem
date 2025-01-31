package com.led.broker.service;

import com.led.broker.model.Agenda;
import com.led.broker.model.Cor;
import com.led.broker.model.Dispositivo;
import com.led.broker.model.Log;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.ModoOperacao;
import com.led.broker.repository.CorRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.repository.OperacaoRepository;
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

    public Cor buscaCor(UUID id){
        return corRepository.findById(id).orElseThrow(() -> new RuntimeException("Cor inválida ou removida"));
    }

    public Mono<String> salvarCorTemporizada(UUID idCor, String mac, boolean cancelar) {

        try{
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mac);

        if(cancelar && dispositivoOptional.isPresent()){
            logger.warn("Cancelando comando");
            Dispositivo dispositivo = dispositivoOptional.get();
            setOperacao(dispositivo);
            operacaoRepository.save(dispositivo.getOperacao());
            logRepository.save(Log.builder()
                    .data(LocalDateTime.now())
                    .usuario("")
                    .mensagem(String.format(Comando.TIMER_CANCELADO.value(), dispositivo.getMac()))
                    .cor(null)
                    .comando(Comando.TIMER_CANCELADO)
                    .descricao(String.format(Comando.TIMER_CANCELADO.value(), dispositivo.getMac()))
                    .mac(dispositivo.getMac())
                    .build());
            dispositivoRepository.save(dispositivo);
           return  comandoService.enviardComandoRapido(dispositivo, true, false);
        }
        else{
            Optional<Cor> corOptional = corRepository.findById(idCor);
            if (dispositivoOptional.isPresent() && corOptional.isPresent()) {
                Dispositivo dispositivo = dispositivoOptional.get();

                dispositivo.getOperacao().setModoOperacao(ModoOperacao.TEMPORIZADOR);
                dispositivo.getOperacao().setTime(LocalDateTime.now().plusMinutes(corOptional.get().getTime()));
                dispositivo.getOperacao().setCorTemporizador(buscaCor(idCor));
                operacaoRepository.save(dispositivo.getOperacao());
                dispositivoRepository.save(dispositivo);
                dispositivo.setCor(corOptional.get());
                TimeUtil.timers.put(dispositivo.getMac(), dispositivo);
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("")
                        .mensagem(String.format(Comando.TIMER_CRIADO.value(), dispositivo.getMac()))
                        .cor(null)
                        .comando(Comando.TIMER_CRIADO)
                        .descricao(String.format(Comando.TIMER_CRIADO.value(), dispositivo.getMac()))
                        .mac(dispositivo.getMac())
                        .build());
                logger.warn("Temporizador criado para " + dispositivo.getMac());
                logger.warn("Efeito " + dispositivo.getCor().getEfeito());
                return   comandoService.enviardComandoRapido(dispositivo, false, false);
            }else{
                logger.error("Falha, cor não existe ou não encontrada");
                return Mono.just("Falha, cor não existe ou não encontrada");
            }
        }}catch (Exception errr){
            logger.error(errr.getMessage());
            return Mono.just("Falha ao enviar comando");
        }
    }


    public void salvarCorTemporizadaReponse(UUID idCor, String mac, boolean cancelar, boolean retentar) {

        try{
            Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mac);

            if(cancelar && dispositivoOptional.isPresent()){
                Dispositivo dispositivo = dispositivoOptional.get();
                setOperacao(dispositivo);

                dispositivoRepository.save(dispositivo);
                comandoService.enviardComandoRapido(dispositivo, true);
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("")
                        .mensagem(String.format(Comando.TIMER_CANCELADO.value(), dispositivo.getMac()))
                        .cor(null)
                        .comando(Comando.TIMER_CANCELADO)
                        .descricao(String.format(Comando.TIMER_CANCELADO.value(), dispositivo.getMac()))
                        .mac(dispositivo.getMac())
                        .build());
            }
            else{
                Optional<Cor> corOptional = corRepository.findById(idCor);
                if (dispositivoOptional.isPresent() && corOptional.isPresent()) {
                    Dispositivo dispositivo = dispositivoOptional.get();

                    dispositivo.getOperacao().setModoOperacao(ModoOperacao.TEMPORIZADOR);
                    dispositivo.getOperacao().setTime(LocalDateTime.now().plusMinutes(-1));
                    dispositivo.getOperacao().setCorTemporizador(buscaCor(idCor));
                    dispositivoRepository.save(dispositivo);
                    dispositivo.setCor(corOptional.get());
                    TimeUtil.timers.put(dispositivo.getMac(), dispositivo);
                     comandoService.enviardComandoRapido(dispositivo, false);
                    logRepository.save(Log.builder()
                            .data(LocalDateTime.now())
                            .usuario("")
                            .mensagem(String.format(Comando.TIMER_CRIADO.value(), dispositivo.getMac()))
                            .cor(null)
                            .comando(Comando.TIMER_CRIADO)
                            .descricao(String.format(Comando.TIMER_CRIADO.value(), dispositivo.getMac()))
                            .mac(dispositivo.getMac())
                            .build());
                }
            }}catch (Exception errr){
            if(retentar){
                System.err.println("Retentativa");
                salvarCorTemporizadaReponse(idCor, mac, false, false);
            }else{
                throw new RuntimeException("Erro ao enviar comando");
            }
        }
    }

    public void setOperacao(Dispositivo dispositivo) {
        Agenda agenda = null;

        dispositivo.getOperacao().setModoOperacao(ModoOperacao.DISPOSITIVO);

        if (Boolean.FALSE.equals(dispositivo.isIgnorarAgenda())) {
            agenda = agendaDeviceService.buscarAgendaDipositivoPrevistaHoje(dispositivo.getMac());
            if(agenda == null){
                List<Agenda> agendasParatodosHoje = agendaDeviceService.listaTodosAgendasPrevistaHoje(true);
                if(!agendasParatodosHoje.isEmpty()){
                    agenda = agendasParatodosHoje.stream().findFirst().get();
                }
            }
            if (agenda != null && agenda.getCor() != null) {
                dispositivo.getOperacao().setModoOperacao(ModoOperacao.TEMPORIZADOR);
                dispositivo.getOperacao().setAgenda(agenda);
            }
        }
    }
}
