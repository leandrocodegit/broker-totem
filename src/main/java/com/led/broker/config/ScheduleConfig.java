package com.led.broker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.led.broker.controller.response.DashboardResponse;
import com.led.broker.model.Log;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.Topico;
import com.led.broker.repository.LogRepository;
import com.led.broker.service.DashboardService;
import com.led.broker.service.DispositivoService;
import com.led.broker.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduleConfig {
    private final DispositivoService dispositivoService;
    private final LogRepository logRepository;
    private final DashboardService dashboardService;
    private Boolean enviarDashBoard = false;
    private final MqttService mqttService;

    @Scheduled(fixedRate = 300000)
    public void checkarDipositivosOffline() {
        dispositivoService.dispositivosQueFicaramOffilne().forEach(device -> {
            logRepository.save(Log.builder()
                    .data(LocalDateTime.now())
                    .usuario("Sistema")
                    .mensagem(device.getMac())
                    .cor(null)
                    .comando(Comando.OFFLINE)
                    .descricao(String.format(Comando.OFFLINE.value(), device.getMac()))
                    .mac(device.getMac())
                    .build());
            dispositivoService.salvarDispositivoComoOffline(device);
            enviarDashBoard = true;
        });
    }

    @Scheduled(fixedRate = 60000)
    public void atualizacaoDashboard() {
        if(Boolean.TRUE.equals(enviarDashBoard)){
            DashboardResponse response = dashboardService.atualizarDashboard("");

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create();
            mqttService.sendRetainedMessage(Topico.TOPICO_DASHBOARD, "Atualizando dashboard", false);
        }
    }
}
