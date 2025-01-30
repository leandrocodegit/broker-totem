package com.led.broker.config;

import com.led.broker.model.Log;
import com.led.broker.model.constantes.Comando;
import com.led.broker.repository.LogRepository;
import com.led.broker.service.ComandoService;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduleConfig {
     private final ComandoService comandoService;
    private final LogRepository logRepository;

    @Scheduled(fixedRate = 10000)
    public void checkTimers() {

        List<String> devicesRemove = new ArrayList<>();

        TimeUtil.timers.values().forEach(device -> {
            if(!TimeUtil.isTime(device)) {
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("Sistema")
                        .mensagem(String.format(Comando.TIMER_CONCLUIDO.value(), device.getMac()))
                        .cor(null)
                        .comando(Comando.TIMER_CONCLUIDO)
                        .descricao(String.format(Comando.TIMER_CONCLUIDO.value(), device.getMac()))
                        .mac(device.getMac())
                        .build());
                devicesRemove.add(device.getMac());
               // comandoService.enviardComandoRapido(device, true, true);
            }
        });

        if(!devicesRemove.isEmpty()){
            devicesRemove.forEach(dev -> {
                TimeUtil.timers.remove(dev);
            });
            devicesRemove.clear();
        }
    }
}
