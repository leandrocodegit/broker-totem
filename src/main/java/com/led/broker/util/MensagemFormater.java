package com.led.broker.util;

import com.led.broker.model.Mensagem;
import static com.led.broker.model.constantes.Comando.*;

import com.led.broker.model.constantes.Comando;
import com.led.broker.model.constantes.Efeito;
import com.led.broker.model.constantes.TipoConexao;
import com.led.broker.model.constantes.TipoConexao.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MensagemFormater {

    public static Mensagem formatarMensagem(String mensagemHexa) {
        //TIPO COMANDO(2) + ID(7) + TIPOCONEXAO(2) + PINO(2) + EFEITO(8) + VERSAO(6)
        //1 ETH
        //2 WIFI
        //3 LORA

        var inicio = 0;
        var comando = Comando.fromDescricao(hexToInt(mensagemHexa.substring(inicio, inicio + 2)));

        if (Stream.of(LORA_PARAMETROS_OK, LORA_PARAMETROS_ERRO, ACEITO, MENSAGEM_PARICIONADA).anyMatch(cmd -> cmd.equals(comando))){
            return Mensagem.builder()
                    .id(hexToInt(mensagemHexa.substring(inicio += 2, inicio + 14)))
                    .comando(comando)
                    .portas(0)
                    .pino(0)
                    .build();
        }

        return Mensagem.builder()
                .comando(comando)
                .id(hexToInt(mensagemHexa.substring(inicio += 2, inicio + 14)))
                .tipoConexao(TipoConexao.fromDescricao(hexToInt(mensagemHexa.substring(inicio += 2, inicio + 2))))
                .portas(hexToInt(mensagemHexa.substring(inicio += 2, inicio + 2)))
                .pino(hexToInt(mensagemHexa.substring(inicio += 2, inicio + 2)))
                .efeito(
                        List.of(
                                Efeito.fromDescricao(hexToInt(mensagemHexa.substring(inicio += 2, inicio + 2))),
                                Efeito.fromDescricao(hexToInt(mensagemHexa.substring(inicio += 2, inicio + 2))),
                                Efeito.fromDescricao(hexToInt(mensagemHexa.substring(inicio += 2, inicio + 2))),
                                Efeito.fromDescricao(hexToInt(mensagemHexa.substring(inicio += 2, inicio + 2)))
                        ))
                .versao(String.format("%d.%d.%d",
                        hexToInt(mensagemHexa.substring(inicio += 2, inicio + 2)),
                        hexToInt(mensagemHexa.substring(inicio += 2, inicio + 2)),
                        hexToInt(mensagemHexa.substring(inicio += 2, inicio + 2))))
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
