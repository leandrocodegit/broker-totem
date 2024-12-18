package com.led.broker.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.led.broker.model.Mensagem;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.Topico;
//import com.led.broker.service.ComandoService;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MqttMessageHandler implements MessageHandler {


    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, String> clientMap = new ConcurrentHashMap<>();

    @Override
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        UUID clientId = (UUID) message.getHeaders().get("id");
        String topico = (String) message.getHeaders().get("mqtt_receivedTopic");

        byte[] bytess = (byte[]) message.getPayload();
        Object payloads = null;
        try {
            payloads = objectMapper.readValue(bytess, Object.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (topico.startsWith(Topico.DEVICE_SEND)) {

            try {
                byte[] bytes = (byte[]) message.getPayload();
                Mensagem payload = objectMapper.readValue(bytes, Mensagem.class);
                payload.setBrockerId(clientId.toString());

//                if(payload.getComando().equals(Comando.ACEITO)){
//                    if(ComandoService.streams.containsKey(payload.getId())){
//                        ComandoService.streams.remove(payload.getId()).success(payload.getComando().value() + " " + payload.getId());
//                    }
//                }

            } catch (Exception erro) {
                System.out.println("Erro ao capturar id");
                erro.printStackTrace();
            }
        }

        System.out.println("Mensagem recebida do cliente " + clientId + ": " + message.getPayload().toString());
    }

    // Método para obter informações de um cliente
    public String getClientInfo(String clientId) {
        return clientMap.get(clientId);
    }
}
