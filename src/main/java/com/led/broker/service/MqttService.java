package com.led.broker.service;

import com.led.broker.config.MqttGateway;
import com.led.broker.model.Conexao;
import com.led.broker.model.KoreMensagem;
import com.led.broker.model.constantes.TipoConexao;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.MonoSink;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MqttService {
    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);
    public static Map<Long, KoreMensagem> mensagensParticionadas = new HashMap<>();
    @Value("${kore-token}")
    private String token;
    private final MqttGateway mqttGateway;
    private final KoreService koreService;


    synchronized public void sendRetainedMessage(String topic, String message) {
        logger.warn("Comando enviado para: " + topic);
        mqttGateway.sendToMqtt(message, topic);
        logger.warn("Mensagem: " + message);
    }

    synchronized public void sendRetainedMessageParticionada(long idDipositivo) {
        var kore = MqttService.mensagensParticionadas.remove(idDipositivo);
        if(kore != null) {
            logger.warn("Enviando mensagem particionada kore: " + kore.toString());
            koreService.enviarMensagem(token, kore);
        }
    }

    synchronized public void sendRetainedMessage(String topic, String message, Conexao conexao) {
        logger.warn("Comando enviado para: " + topic);
        if (conexao.getTipoConexao().equals(TipoConexao.LORA)) {

            StringBuilder output = new StringBuilder();
            for (char c : message.toCharArray()) {
                output.append("0").append(c);
            }

            if (message.length() > 50 && conexao.getFracionarMensagem() != null && conexao.getFracionarMensagem().equals(Boolean.TRUE)) {
                int fracao = message.length() / 2;
                String parte1 = message.substring(0, fracao);
                String parte2 = message.substring(fracao);
                var kore = KoreMensagem.builder()
                        .devEUI(conexao.getDevEui())
                        .payload(Base64.getEncoder().encodeToString(parte1.getBytes()))
                        .port(2)
                        .build();
                logger.warn("Enviando mensagem para kore: " + kore.toString());
                koreService.enviarMensagem(token, kore);

                MqttService.mensagensParticionadas.put(Long.parseLong(conexao.getId()), KoreMensagem.builder()
                        .devEUI(conexao.getDevEui())
                        .payload(Base64.getEncoder().encodeToString(parte2.getBytes()))
                        .port(3)
                        .build());
            } else {
                var kore = KoreMensagem.builder()
                        .devEUI(conexao.getDevEui())
                        .payload(Base64.getEncoder().encodeToString(message.getBytes()))
                        .port(1)
                        .build();
                logger.warn("Enviando mensagem para kore: " + kore.toString());
                koreService.enviarMensagem(token, kore);
            }
        } else {
            mqttGateway.sendToMqtt(message, topic);
        }
        logger.warn("Mensagem: " + message);
    }

    synchronized public void sendRetainedMessage(String topic, byte[] message) {
        logger.warn("Comando enviado para: " + topic);

        Message<byte[]> payload = MessageBuilder
                .withPayload(message) // Envia um array de bytes
                .setHeader(MqttHeaders.TOPIC, topic)
                .build();
        mqttGateway.sendToMqtt(payload);
        logger.warn("Mensagem: " + message.toString());
    }
}
