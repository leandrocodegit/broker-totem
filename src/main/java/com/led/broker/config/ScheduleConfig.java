package com.led.broker.config;

import com.led.broker.model.Agenda;
import com.led.broker.model.constantes.Topico;
import com.led.broker.service.AgendaDeviceService;
import com.led.broker.service.ComandoService;
import com.led.broker.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduleConfig {

    private final AgendaDeviceService agendaDeviceService;
    private final ComandoService comandoService;
    private final MqttService mqttService;

    @Scheduled(fixedRate = 2 * 60 * 1000)
    public void executarTarefaAgendada() {
        List<Agenda> agendas = agendaDeviceService.listaTodosAgendasPrevistaHoje();
        System.out.println("Agendas #: " + agendas.size());
        if(!agendas.isEmpty()){

            agendas.forEach(agenda -> {
                System.out.println(agenda.getInicio() + " : " + agenda.getTermino());
                System.out.println("Execução: " + agenda.getExecucao());
                comandoService.enviarComando(agenda);
                agendaDeviceService.atualizarDataExecucao(agenda);

            });
            mqttService.sendRetainedMessage(Topico.DASHBOARD, "Atualizar dashboard agenda", false);
         }
    }
}
