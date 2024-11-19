package com.led.broker.config;

import com.led.broker.model.Agenda;
import com.led.broker.model.Log;
import com.led.broker.model.constantes.Comando;
import com.led.broker.repository.LogRepository;
import com.led.broker.service.AgendaDeviceService;
import com.led.broker.service.ComandoService;
import com.led.broker.service.DispositivoService;
import com.led.broker.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableScheduling
public class ScheduleConfig {

    @Autowired
    private AgendaDeviceService agendaDeviceService;
    @Autowired
    private ComandoService comandoService;
    @Autowired
    private DispositivoService dispositivoService;
    @Autowired
    private LogRepository logRepository;

    @Scheduled(fixedRate = 5000)
    public void executarTarefaAgendada() {
        List<Agenda> agendas = agendaDeviceService.listaTodosAgendasPrevistaHoje();


        if(!agendas.isEmpty()){
            System.out.println("# " + agendas.size());
            agendas.forEach(agenda -> {
                System.out.println(agenda.getDispositivos().toString());
                comandoService.enviarComando(agenda);
                agendaDeviceService.atualizarDataExecucao(agenda);
            });
            System.out.println("Tarefa executada a cada 5 segundos: " + System.currentTimeMillis());
         }
    }

    @Scheduled(fixedRate = 360000)
    public void checkarDipositivosOffile() {
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
            System.out.println("Dispositivo offline " + device.getMac());
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
                System.out.println("Timer finalizado " + device.getMac());
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
