package com.led.broker.config;

import com.led.broker.controller.response.DashboardResponse;
import com.led.broker.model.Agenda;
import com.led.broker.model.Log;
import com.led.broker.model.constantes.Comando;
import com.led.broker.repository.LogRepository;
import com.led.broker.service.*;
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

    private final AgendaDeviceService agendaDeviceService;
    private final ComandoService comandoService;
    private final DispositivoService dispositivoService;
    private final LogRepository logRepository;
    private final WebSocketService webSocketService;
    private final DashboardService dashboardService;
    private Boolean enviarDashBoard = false;

    @Scheduled(fixedRate = 5000)
    public void executarTarefaAgendada() {
        List<Agenda> agendas = agendaDeviceService.listaTodosAgendasPrevistaHoje();

        if(!agendas.isEmpty()){
            agendas.forEach(agenda -> {
                comandoService.enviarComando(agenda);
                agendaDeviceService.atualizarDataExecucao(agenda);
            });
            enviarDashBoard = true;
         }
    }

    @Scheduled(fixedRate = 360000)
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

    @Scheduled(fixedRate = 1000)
    public void checkTimers() {

        List<String> devicesRemove = new ArrayList<>();

        TimeUtil.timers.values().forEach(device -> {
            if(!TimeUtil.isTime(device)) {
                logRepository.save(Log.builder()
                        .data(LocalDateTime.now())
                        .usuario("Sistema")
                        .mensagem(String.format(Comando.TIMER_CRIADO.value(), device.getMac()))
                        .cor(null)
                        .comando(Comando.TIMER_CONCLUIDO)
                        .descricao(String.format(Comando.TIMER_CONCLUIDO.value(), device.getMac()))
                        .mac(device.getMac())
                        .build());
                devicesRemove.add(device.getMac());
                comandoService.enviardComandoRapido(device, true, true);
                enviarDashBoard = true;
            }
        });

        if(!devicesRemove.isEmpty()){
            devicesRemove.forEach(dev -> {
                TimeUtil.timers.remove(dev);
            });
            devicesRemove.clear();
        }
    }

    @Scheduled(fixedRate = 30000)
    public void atualizacaoDashboard() {
        if(Boolean.TRUE.equals(enviarDashBoard)){
            System.out.println("Atualizando dashboard");
            DashboardResponse response = dashboardService.atualizarDashboard("");
             webSocketService.sendMessageDashboard(response);
            enviarDashBoard = false;
        }
    }
}
