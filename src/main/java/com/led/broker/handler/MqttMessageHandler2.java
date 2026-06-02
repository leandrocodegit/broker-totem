package com.led.broker.handler;

import com.google.gson.Gson;
import com.led.broker.handler.model.MensagemRecebida;
import com.led.broker.handler.model.TipoMensagem;
import com.led.broker.model.Mensagem;
import com.led.broker.service.ComandoService;
import com.led.broker.service.MqttService;
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

import static com.led.broker.model.constantes.Comando.*;

@Component
@RequiredArgsConstructor
public class MqttMessageHandler2 implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MqttMessageHandler2.class);
    private final ConcurrentHashMap<String, String> clientMap = new ConcurrentHashMap<>();
    private final MqttService mqttService;
    private final LoRaDecrypt loRaDecrypt;

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
            logger.error("Mensagem padrão");

            MensagemRecebida payload = new Gson().fromJson(message.getPayload().toString(), MensagemRecebida.class);
            if (payload.getTipo().equals(TipoMensagem.ACEITO) && ComandoService.streams.containsKey(payload.getId())) {
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
