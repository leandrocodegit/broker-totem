package com.led.broker.util;

import com.led.broker.model.Configuracao;
import com.led.broker.model.Parametro;
import com.led.broker.model.constantes.Efeito;

import java.util.ArrayList;
import java.util.List;

public class MensagemFormater {

    public static Parametro recuperarConfiguracao(String mensagemHexa) {
        // EFEITO(1) + PINO(1) + LEDS(4) + FAIXA(4) + INTENSIDADE(2) + VELOCIDADE(2)
        // CORRECAO 1 + CORRECAO 2 + CORRECAO 3 + COR 1 + COR 2 + COR 3 + COR 4 + COR 5 + COR 6
        // 01 + 1 + 1388 + 1388 + FF + FF;


        Parametro parametro = Parametro.builder()
                .efeito(Efeito.fromDescricao(hexToInt(mensagemHexa.substring(0, 2))))
                .pino(hexToInt(mensagemHexa.substring(2, 4)))
                .configuracao(Configuracao.builder()
                        .leds(hexToInt(mensagemHexa.substring(4, 8)))
                        .faixa(hexToInt(mensagemHexa.substring(8, 12)))
                        .intensidade(hexToInt(mensagemHexa.substring(12, 14)))
                        .build())
                .correcao(vetor(mensagemHexa.substring(16, 22)))
                .cor(vetor(mensagemHexa.substring(22, 34)))
                .build();

        return parametro;
    }

    public static int[] vetor(String hexa) {
        var tamanho = hexa.length() / 2;
        var params = new int[tamanho];

        List<String> codigos = new ArrayList<>();
        List<Integer> convertidos = new ArrayList<>();
        for (int i = 0; i < hexa.length(); i += 2) {
            codigos.add(hexa.substring(i, i + 2));
        }

        for (int i = 0; i < codigos.size(); i++) {
            params[i] = hexToInt(codigos.get(i));
        }

        return params;
    }

    public static int hexToInt(String hex) {
        return Integer.parseInt(hex, 16);
    }

}
