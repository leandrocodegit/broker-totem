package com.led.broker.util;


import com.led.broker.controller.request.ComandoRequest;
import com.led.broker.model.Configuracao;
import com.led.broker.model.Cor;
import com.led.broker.model.Dispositivo;
import com.led.broker.model.constantes.Efeito;
import com.led.broker.model.constantes.TipoCor;

public class ConfiguracaoUtil {

    private ConfiguracaoUtil() {}

    public static ComandoRequest gerarComando(Cor cor, Configuracao configuracao ){
        return ComandoRequest.builder()
                .efeito(cor.getEfeito())
                .cor(getCor(cor.getCor(), configuracao.getTipoCor()))
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
                .cor(getCor(cor.getCor(), dispositivo.getConfiguracao().getTipoCor()))
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
                .cor(getCor(dispositivo.getCor().getCor(), dispositivo.getConfiguracao().getTipoCor()))
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
                .cor(getCor(dispositivo.getCor().getCor(), dispositivo.getConfiguracao().getTipoCor()))
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

    private static int [] getCor(int[] cores, TipoCor tipoCor){
        if(tipoCor.equals(TipoCor.RBG)){
            return new int[] {
                    cores[0],
                    cores[1],
                    cores[2],
                    cores[6],
                    cores[7],
                    cores[8],
                    cores[3],
                    cores[4],
                    cores[6]
            };
        }else if(tipoCor.equals(TipoCor.GRB)){
            return new int[] {
                    cores[3],
                    cores[4],
                    cores[6],
                    cores[0],
                    cores[1],
                    cores[2],
                    cores[6],
                    cores[7],
                    cores[8]
            };
        }
        return cores;
    }
}
