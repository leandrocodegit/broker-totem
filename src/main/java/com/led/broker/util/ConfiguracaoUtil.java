package com.led.broker.util;


import com.led.broker.controller.response.ComandoRequest;
import com.led.broker.model.Configuracao;
import com.led.broker.model.Cor;
import com.led.broker.model.Dispositivo;
import com.led.broker.model.constantes.Efeito;

public class ConfiguracaoUtil {


    public static ComandoRequest gerarComando(Cor cor, Configuracao configuracao ){
        return ComandoRequest.builder()
                .efeito(cor.getEfeito())
                .cor(cor.getCor())
                .correcao(cor.getCorrecao())
                .velocidade(cor.getVelocidade())
                .responder(cor.isResponder())
                .faixa(configuracao.getFaixa())
                .leds(configuracao.getLeds())
                .intensidade(configuracao.getIntensidade()).build();
    }

    public static ComandoRequest gerarComando(Dispositivo dispositivo, Cor cor){
        return ComandoRequest.builder()
                .efeito(cor.getEfeito())
                .cor(cor.getCor())
                .correcao(cor.getCorrecao())
                .velocidade(cor.getVelocidade())
                .responder(cor.isResponder())
                .faixa(dispositivo.getConfiguracao().getFaixa())
                .leds(dispositivo.getConfiguracao().getLeds())
                .intensidade(dispositivo.getConfiguracao().getIntensidade()).build();
    }

    public static ComandoRequest gerarComando(Dispositivo dispositivo){
        return ComandoRequest.builder()
                .efeito(dispositivo.getCor().getEfeito())
                .cor(dispositivo.getCor().getCor())
                .correcao(dispositivo.getCor().getCorrecao())
                .velocidade(dispositivo.getCor().getVelocidade())
                .responder(true)
                .faixa(dispositivo.getConfiguracao().getFaixa())
                .leds(dispositivo.getConfiguracao().getLeds())
                .intensidade(dispositivo.getConfiguracao().getIntensidade()).build();
    }
    public static ComandoRequest gerarComando(Dispositivo dispositivo, boolean responder){
        return ComandoRequest.builder()
                .efeito(dispositivo.getCor().getEfeito())
                .cor(dispositivo.getCor().getCor())
                .correcao(dispositivo.getCor().getCorrecao())
                .velocidade(dispositivo.getCor().getVelocidade())
                .responder(responder)
                .faixa(dispositivo.getConfiguracao().getFaixa())
                .leds(dispositivo.getConfiguracao().getLeds())
                .intensidade(dispositivo.getConfiguracao().getIntensidade()).build();
    }

    public static ComandoRequest gerarComandoTeste(Configuracao configuracao){
        return ComandoRequest.builder()
                .efeito(Efeito.TESTE)
                .cor(new int[]{0,0,0,0,0,0})
                .correcao(new int[]{255,255,255,255,255,255})
                .velocidade(100)
                .responder(true)
                .faixa(2)
                .leds(configuracao.getLeds())
                .intensidade(255).build();
    }
}
