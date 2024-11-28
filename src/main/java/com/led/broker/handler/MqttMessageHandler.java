package com.led.broker.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@Component
@RequiredArgsConstructor
public class MqttMessageHandler implements MessageHandler {


    private final DispositivoService dispositivoService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, String> clientMap = new ConcurrentHashMap<>();

    @Override
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        String topico = (String) message.getHeaders().get("mqtt_receivedTopic");

        if (topico.length() > Topico.DEVICE_CONFIRMACAO.length()) {
            try {
                String id = topico.replace(Topico.DEVICE_CONFIRMACAO, "");
                ComandoService.streams.remove(id).success(Comando.ACEITO.value() + " " + id);
            } catch (Exception erro) {
                if(message != null && message.getPayload() != null)
                    System.out.println("Erro ao confirmar resposta" + message.getPayload().toString());
            }
        }

    }

    // Método para obter informações de um cliente
    public String getClientInfo(String clientId) {
        return clientMap.get(clientId);
    }
}
