package com.led.broker.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.led.broker.model.Mensagem;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.Topico;
import com.led.broker.service.ComandoService;
import com.led.broker.service.DispositivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class MqttMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MqttMessageHandler.class);

    private final DispositivoService dispositivoService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final ConcurrentHashMap<String, Future<?>> tasks = new ConcurrentHashMap<>();


    @Override
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        UUID clientId = (UUID) message.getHeaders().get("id");
        //String topico = (String) message.getHeaders().get("mqtt_receivedTopic");

        try {
            Mensagem payload = new Gson().fromJson(message.getPayload().toString(), Mensagem.class);
            if (payload.getComando().equals(Comando.ONLINE) || payload.getComando().equals(Comando.CONFIGURACAO) || payload.getComando().equals(Comando.CONCLUIDO)) {
            payload.setBrockerId(clientId.toString());
                processarDispositivo(payload.getId(), payload);
            }
        } catch (Exception erro) {
            logger.error("Erro ao capturar id");
        }
        logger.warn("Mensagem recebida do cliente " + clientId + ": " + message.getPayload().toString());
    }

    private void processarDispositivo(String id, Mensagem payload) {
        Future<?> existingTask = tasks.put(id, executorService.submit(() -> {
            try {
                dispositivoService.atualizarDispositivo(payload);
            } catch (Exception e) {
                logger.error("Erro ao atualizar dispositivo {}", id, e);
            } finally {
                tasks.remove(id);
            }
        }));

        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(true);
        }
    }

}
