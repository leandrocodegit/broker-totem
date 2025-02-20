package com.led.broker.util;

import com.led.broker.model.Configuracao;
import com.led.broker.model.Mensagem;
import com.led.broker.model.Parametro;
import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.Efeito;
import com.led.broker.model.constantes.TipoConexao;

import java.util.ArrayList;
import java.util.List;

public class MensagemFormater {

    public static Mensagem formatarMensagem(String mensagemHexa) {
        //TIPO COMANDO(2) + ID(7) + TIPOCONEXAO(2) + PINO(2) + EFEITO(8) + VERSAO(6)
        //1 ETH
        //2 WIFI
        //3 LORA

      return   Mensagem.builder()
                .comando(Comando.fromDescricao(hexToInt(mensagemHexa.substring(0, 2))))
                .id(hexToInt(mensagemHexa.substring(2, 16)))
                .tipoConexao(TipoConexao.fromDescricao(hexToInt(mensagemHexa.substring(16, 18))))
                .pino(hexToInt(mensagemHexa.substring(18, 20)))
                .efeito(Efeito.fromDescricao(hexToInt(mensagemHexa.substring(20, 22))))
                .versao(String.format("%d.%d.%d",
                        hexToInt(mensagemHexa.substring(22, 24)),
                        hexToInt(mensagemHexa.substring(24, 26)),
                        hexToInt(mensagemHexa.substring(26, 28))))
                .build();

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
