package com.led.broker.config;

import com.led.broker.service.ComandoService;
import com.led.broker.service.CorService;
import com.led.broker.service.MqttService;
import com.led.broker.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;

import static com.led.broker.model.constantes.Topico.DASHBOARD;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduleConfig {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleConfig.class);
    private final MqttService mqttService;
    private final CorService corService;

    @Scheduled(fixedRate = 1 * 60 * 1000)
    public void atualizarDashboardAoSincronizarTodos() {

        if (!ComandoService.clientes.isEmpty()) {
            logger.info("Atualizando dashboard de clientes: " + ComandoService.clientes.size());
            var clientes =  ComandoService.clientes.values();
            clientes.forEach(cliente -> {
                var clienteId = ComandoService.clientes.remove(cliente.toString());
                mqttService.sendRetainedMessage(DASHBOARD + "/" + clienteId, "Atualizando dashboard");
            });
        }
    }

    @Scheduled(fixedRate = 2 * 1000)
    public void checkTimers() {
        logger.info("Checando timers: " + TimeUtil.timers.size());
        List<Long> devicesRemove = new ArrayList<>();
        TimeUtil.timers.values().forEach(device -> {
            if(TimeUtil.isTimeTemporizador(device)) {
                corService.cancelarComando(device.getDispositivoEntity(), "Sistema");
                devicesRemove.add(device.getDispositivoEntity().getId());
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
