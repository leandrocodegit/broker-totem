package com.led.broker.service;

import com.led.broker.model.Cor;
import com.led.broker.model.Dispositivo;
import com.led.broker.model.Log;
import com.led.broker.model.Temporizador;
import com.led.broker.model.constantes.Comando;
import com.led.broker.repository.CorRepository;
import com.led.broker.repository.DispositivoRepository;
import com.led.broker.repository.LogRepository;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CorService {

    private final CorRepository corRepository;
    private final DispositivoRepository dispositivoRepository;
    private final ComandoService comandoService;
    private final LogRepository logRepository;

    public Optional<Cor> buscaCor(UUID id){
        return corRepository.findById(id);
    }

    public Mono<String> salvarCorTemporizada(UUID idCor, String mac, boolean cancelar) {

        try{
        Optional<Dispositivo> dispositivoOptional = dispositivoRepository.findById(mac);

        if(cancelar && dispositivoOptional.isPresent()){
            Dispositivo dispositivo = dispositivoOptional.get();
            dispositivo.setTemporizador(Temporizador.builder()
                    .idCor(idCor)
                    .time(LocalDateTime.now().plusMinutes(-1))
                    .build());

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

                dispositivo.setTemporizador(Temporizador.builder()
                        .idCor(idCor)
                        .time(LocalDateTime.now().plusMinutes(corOptional.get().getTime()))
                        .build());

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
                return   comandoService.enviardComandoRapido(dispositivo, false, false);
            }
        }}catch (Exception errr){
            return Mono.just("Falha ao enviar comando");
        }
        return Mono.empty();
    }

}
