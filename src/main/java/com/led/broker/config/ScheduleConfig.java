package com.led.broker.config;

import com.led.broker.model.constantes.Topico;
import com.led.broker.service.ComandoService;
import com.led.broker.service.DispositivoService;
import com.led.broker.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import static com.led.broker.model.constantes.Topico.DASHBOARD;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduleConfig {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleConfig.class);
    private final MqttService mqttService;

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
}
