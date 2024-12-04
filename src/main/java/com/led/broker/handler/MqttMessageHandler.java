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

@Component
@RequiredArgsConstructor
public class MqttMessageHandler implements MessageHandler {


    private final DispositivoService dispositivoService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final ConcurrentHashMap<String, String> clientMap = new ConcurrentHashMap<>();


    @Override
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        UUID clientId = (UUID) message.getHeaders().get("id");
        //String topico = (String) message.getHeaders().get("mqtt_receivedTopic");

        try {
            Mensagem payload = new Gson().fromJson(message.getPayload().toString(), Mensagem.class);
            if (payload.getComando().equals(Comando.ONLINE) || payload.getComando().equals(Comando.CONFIGURACAO) || payload.getComando().equals(Comando.CONCLUIDO)) {
            payload.setBrockerId(clientId.toString());
                executorService.submit(() -> {
                    try {
                        dispositivoService.atualizarDispositivo(payload);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Erro ao tratar mensagem");
                    }
                });
           // dispositivoService.atualizarDispositivo(payload);
            }

        } catch (Exception erro) {
            System.out.println("Erro ao capturar id");
        }

        System.out.println("Mensagem recebida do cliente " + clientId + ": " + message.getPayload().toString());
    }

    // Método para obter informações de um cliente
    public String getClientInfo(String clientId) {
        return clientMap.get(clientId);
    }
}
