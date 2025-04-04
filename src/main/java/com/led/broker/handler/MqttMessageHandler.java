package com.led.broker.handler;

import com.google.gson.Gson;
import com.led.broker.model.Mensagem;
import static com.led.broker.model.constantes.Comando.*;
import com.led.broker.service.ComandoService;
import com.led.broker.util.MensagemFormater;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class MqttMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MqttMessageHandler.class);
    private final ConcurrentHashMap<String, String> clientMap = new ConcurrentHashMap<>();

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b)); // Converte cada byte para 2 caracteres hexadecimais
        }
        return hexString.toString();
    }
    @Override
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        UUID clientId = (UUID) message.getHeaders().get("id");
        String topico = (String) message.getHeaders().get("mqtt_receivedTopic");

        try {
            Mensagem payload = MensagemFormater.formatarMensagem(message.getPayload().toString());
            if (Stream.of(LORA_PARAMETROS_OK, LORA_PARAMETROS_ERRO, ACEITO).anyMatch(cmd -> cmd.equals(payload.getComando())) && ComandoService.streams.containsKey(payload.getId())) {
                logger.warn("Payload: " + payload.toString());
                ComandoService.streams.remove(payload.getId()).success(ACEITO.value + " " + payload.getId());
            }
        } catch (Exception erro) {
            if (message != null && message.getPayload() != null)
                logger.warn("Erro ao confirmar resposta" + message.getPayload().toString());
        }

    }

    // Método para obter informações de um cliente
    public String getClientInfo(String clientId) {
        return clientMap.get(clientId);
    }
}
