package com.led.broker.handler;

import com.google.gson.Gson;
import com.led.broker.model.Mensagem;
import com.led.broker.model.constantes.Comando;
import com.led.broker.service.ComandoService;
import com.led.broker.util.MensagemFormater;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

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
        String topico = (String) message.getHeaders().get("mqtt_receivedTopic");
        logger.warn("Mensagem recebida: " + topico);
        byte[] payloadD = (byte[]) message.getPayload();
        String hex = bytesToHex(payloadD);
        System.err.println("Recebi: " + hex);

        var parametro = MensagemFormater.recuperarConfiguracao(hex);

        try {
            Mensagem payload = new Gson().fromJson(message.getPayload().toString(), Mensagem.class);
            if (payload.getComando().equals(Comando.ACEITO) && ComandoService.streams.containsKey(payload.getId())) {
                logger.warn("Payload: " + payload.toString());
                ComandoService.streams.remove(payload.getId()).success(Comando.ACEITO.value() + " " + payload.getId());
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
