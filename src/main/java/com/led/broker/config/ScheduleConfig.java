package com.led.broker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.led.broker.controller.response.DashboardResponse;
import com.led.broker.model.Conexao;
import com.led.broker.model.Dispositivo;
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
import java.util.List;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduleConfig {
    private final DispositivoService dispositivoService;
    private final LogRepository logRepository;
    private final DashboardService dashboardService;
    private Boolean enviarDashBoard = false;
    private final MqttService mqttService;
    private List<Conexao> conexoes;

    @Scheduled(fixedRate = 7 * 60 * 1000)
    public void checkarDipositivosOffline() {

        conexoes = dispositivoService.dispositivosQueFicaramOffilne();
        enviarDashBoard = true;
        dispositivoService.salvarDispositivoComoOffline(conexoes);
        System.out.println("Conexoes: " + conexoes.size());
    }

    @Scheduled(fixedRate = 8 * 60 * 1000)
    public void atualizacaoDashboard() {
        if(Boolean.TRUE.equals(enviarDashBoard)){
            dispositivoService.salvarDispositivoComoOffline(conexoes);
            dashboardService.atualizarDashboard("");
            mqttService.sendRetainedMessage(Topico.TOPICO_DASHBOARD, "Atualizando dashboard");
            enviarDashBoard = false;
        }
    }
}
