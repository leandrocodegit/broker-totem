package com.led.broker.util;

import com.led.broker.controller.request.DispositivoRequest;
import com.led.broker.integracao.model.Dispositivo;
import com.led.broker.model.Cor;
import com.led.broker.model.DispositivoEntity;
import com.led.broker.model.Parametro;
import com.led.broker.model.constantes.ModoOperacao;
import com.led.broker.model.constantes.TipoConexao;
import com.led.broker.model.constantes.TipoConfiguracao;
import com.led.broker.model.constantes.TipoCor;

public class ComandoFormater {


    public static String gerarCodigoFirmware(String host) {
        StringBuilder codigo = new StringBuilder();
        codigo.append(toHexa(TipoConfiguracao.UPDATE.codigo));
        codigo.append(host);
        String tamanho = toHexa(codigo.toString().length());
        return (tamanho + codigo.toString() + tamanho);
    }

    // EFEITO(2) + PINO(1) + LEDS(4) + FAIXA(4) + INTENSIDADE(2) + VELOCIDADE(2)

    public static String gerarCodigoLora(DispositivoEntity dispositivoEntity, boolean responder, TipoConfiguracao tipoConfiguracao) {

        var conexao = dispositivoEntity.getConexao();
        StringBuilder codigo = new StringBuilder();


        codigo.append(toHexa(tipoConfiguracao.codigo));
        if (tipoConfiguracao.equals(TipoConfiguracao.LORA_WAN_RESET)) {
            String tamanho = toHexa(codigo.toString().length());
            return (tamanho + codigo.toString() + tamanho).toUpperCase();
        }

        codigo.append(toHexa(conexao.getHabilitarLoraWan() ? 1 : 0));
        codigo.append(toHexa(conexao.getModoLora()));
        codigo.append(toHexa(conexao.getClasse().codigo));

        codigo.append(toHexa(conexao.getTxPower()));
        codigo.append(toHexa(conexao.getDataRate()));
        codigo.append(toHexa(conexao.getAdr() ? 1 : 0));


        if (tipoConfiguracao.equals(TipoConfiguracao.LORA_WAN)) {

            if (conexao.getModoLora() == 0) {
                codigo.append(conexao.getNwkSKey().replaceAll(":", ""));
                codigo.append(conexao.getAppSKey().replaceAll(":", ""));
                codigo.append(conexao.getDevAddr().replaceAll(":", ""));
            } else {
                codigo.append(toHexa(conexao.getAutoJoin() != null && conexao.getAutoJoin() ? 1 : 0));
                codigo.append(conexao.getAppEui().replaceAll(":", ""));
                codigo.append(conexao.getAppKey().replaceAll(":", ""));
            }
        }

        String tamanho = toHexa(codigo.toString().length());

        return (tamanho + codigo.toString() + tamanho).toUpperCase();
    }

    public static String gerarConfiguracaoId(long id) {
        StringBuilder codigo = new StringBuilder();
        codigo.append(toHexa(TipoConfiguracao.ID.codigo));
        codigo.append(String.format("%014X", id));
        System.err.println(codigo.toString());
        String tamanho = toHexa(codigo.toString().length());
        return (tamanho + codigo.toString() + tamanho).toUpperCase();
    }

    public static String gerarCodigoErase(Dispositivo dispositivoEntity) {
        StringBuilder codigo = new StringBuilder();
        codigo.append(toHexa(TipoConfiguracao.ID.codigo));
        codigo.append(toHexa(TipoConfiguracao.LIMPAR_FLASH.codigo));
        String tamanho = toHexa(codigo.toString().length());
        return (tamanho + codigo.toString() + tamanho).toUpperCase();
    }

    public static String gerarCodigoWIFI(Dispositivo dispositivoEntity) {
        StringBuilder codigo = new StringBuilder();
        codigo.append(toHexa(TipoConfiguracao.WIFI.codigo));
        codigo.append(toHexa(dispositivoEntity.getConexao().getHabilitarWifi() ? 1 : 0));

        for (char c : dispositivoEntity.getConexao().getSsid().toCharArray()) {
            codigo.append(toHexa((int) c));
        }
        codigo.append("20");
        for (char c : dispositivoEntity.getConexao().getSenha().toCharArray()) {
            codigo.append(toHexa((int) c));
        }
        codigo.append("20");
        String tamanho = toHexa(codigo.toString().length());
        return (tamanho + codigo.toString() + tamanho).toUpperCase();
    }

    public static String gerarCodigo(Dispositivo dispositivoEntity, boolean responder, TipoConfiguracao tipoConfiguracao) {

        if (tipoConfiguracao.equals(TipoConfiguracao.LED) || tipoConfiguracao.equals(TipoConfiguracao.LED_RESTART))
            return gerarCodigoCor(dispositivoEntity, responder, tipoConfiguracao);
        else if (tipoConfiguracao.equals(TipoConfiguracao.VIBRACAO))
            return gerarCodigoCor(dispositivoEntity, responder, tipoConfiguracao);
        else if (tipoConfiguracao.equals(TipoConfiguracao.LORA_WAN) || tipoConfiguracao.equals(TipoConfiguracao.LORA_WAN_PARAM))
            return gerarCodigoLora(dispositivoEntity, responder, tipoConfiguracao);

        return "";
    }

    public static String gerarCodigoCor(DispositivoRequest dispositivo) {

        Cor cor = dispositivo.getCor();
        StringBuilder codigo = new StringBuilder();

        codigo.append(toHexa(TipoConfiguracao.LED.codigo));
        codigo.append(toHexa(cor.getParametros().size()));
        codigo.append(toHexa(0));
        codigo.append(toHexa(cor.getVelocidade()));
        codigo.append(toHexa(5));
        codigo.append("00000000");

        cor.getParametros().forEach(parametro -> {
            codigo.append(gerarTextoConfiguracaoLeds(parametro));
            formatarPadraoCor(parametro, dispositivo.getCor());
            codigo.append(gerarTextoParametros(parametro));
        });

        System.err.println(codigo.toString().toUpperCase());
        String tamanho = toHexa(codigo.toString().length());

        return (tamanho + codigo.toString() + tamanho).toUpperCase();
    }

    public static String gerarCodigoCor(Dispositivo dispositivoEntity, boolean responder, TipoConfiguracao tipoConfiguracao) {

        Cor cor = null;
        if (tipoConfiguracao.equals(TipoConfiguracao.VIBRACAO) && dispositivoEntity.getOperacao().getCorVibracao() != null) {
            cor = CorUtil.parametricarCorDispositivoOperacao(dispositivoEntity.getOperacao().getCorVibracao(), dispositivoEntity);
        }else{
          cor = dispositivoEntity.getCor();
        }

        if(dispositivoEntity.getOperacao().getModoOperacao().equals(ModoOperacao.TEMPORIZADOR))
            cor.setVelocidade(dispositivoEntity.getOperacao().getCorTemporizador().getVelocidade());
        if(dispositivoEntity.getOperacao().getModoOperacao().equals(ModoOperacao.AGENDA))
            cor.setVelocidade(dispositivoEntity.getOperacao().getAgenda().getCor().getVelocidade());

        StringBuilder codigo = new StringBuilder();

        codigo.append(toHexa(tipoConfiguracao.codigo));
        codigo.append(toHexa(cor.getParametros().size()));
        codigo.append(responder && !dispositivoEntity.getConexao().getTipoConexao().equals(TipoConexao.LORA) ? toHexa(1) : toHexa(0));
        codigo.append(toHexa(cor.getVelocidade()));
        codigo.append(toHexa(dispositivoEntity.getConexao().getTempoAtividade() == null ? 3 : dispositivoEntity.getConexao().getTempoAtividade()));
        if (dispositivoEntity.getSensibilidadeVibracao() == null || dispositivoEntity.getSensibilidadeVibracao() == 0 || dispositivoEntity.getOperacao().getCorVibracao() == null) {
            codigo.append("00000000");
        } else {
            int bits = Float.floatToIntBits(dispositivoEntity.getSensibilidadeVibracao());
            codigo.append(String.format("%08X", bits));
        }

        cor.getParametros().forEach(parametro -> {
            codigo.append(gerarTextoConfiguracaoLeds(parametro));
            formatarPadraoCor(parametro, dispositivoEntity);
            codigo.append(gerarTextoParametros(parametro));
        });


        System.err.println(codigo.toString().toUpperCase());
        String tamanho = toHexa(codigo.toString().length());

        return (tamanho + codigo.toString() + tamanho).toUpperCase();
    }

    private static String gerarTextoConfiguracaoLeds(Parametro parametro) {
        // EFEITO(2) + PINO(2) + TIPO(2)  + LEDS(4) + FAIXA(4) + INTENSIDADE(2) + VELOCIDADE(2)
        // CORRECAO(3) + COR (6)
        // 01 + 1 + 1388 + 1388 + FF + FF;
        // A113881388FFFF

        StringBuilder codigo = new StringBuilder();
        codigo.append(toHexa(parametro.getEfeito().codigo));
        codigo.append(toHexa(parametro.getPino()));
        codigo.append(toHexa(parametro.getConfiguracao().getLeds(), true));
        codigo.append(toHexa(parametro.getConfiguracao().getFaixa(), true));
        codigo.append(toHexa(parametro.getConfiguracao().getIntensidade()));

        return codigo.toString();
    }

    private static String gerarTextoParametros(Parametro parametro) {
        // EFEITO(2) + PINO(2) + TIPO(2)  + LEDS(4) + FAIXA(4) + INTENSIDADE(2) + VELOCIDADE(2)
        // CORRECAO(3) + COR (6)
        // A + 1 + 1388 + 1388 + FF + FF;
        // A113881388FFFF

        StringBuilder codigo = new StringBuilder();
        codigo.append(toHexa(parametro.getCorrecao()[0]));
        codigo.append(toHexa(parametro.getCorrecao()[1]));
        codigo.append(toHexa(parametro.getCorrecao()[2]));

        codigo.append(toHexa(parametro.getCor()[0]));
        codigo.append(toHexa(parametro.getCor()[1]));
        codigo.append(toHexa(parametro.getCor()[2]));
        codigo.append(toHexa(parametro.getCor()[3]));
        codigo.append(toHexa(parametro.getCor()[4]));
        codigo.append(toHexa(parametro.getCor()[5]));
        codigo.append(toHexa(parametro.getCor()[6]));
        codigo.append(toHexa(parametro.getCor()[7]));
        codigo.append(toHexa(parametro.getCor()[8]));

        return codigo.toString();
    }

    private static void formatarPadraoCor(Parametro parametro, DispositivoEntity dispositivoEntity) {
        formatarPadraoCor(parametro, dispositivoEntity.getCor());
    }
    private static void formatarPadraoCor(Parametro parametro, Cor cor) {
        var parametroDispositivo = cor.getParametros().stream().filter(param -> param.getPino() == parametro.getPino()).findFirst();
        var R1 = parametro.getCor()[0];
        var G1 = parametro.getCor()[1];
        var B1 = parametro.getCor()[2];
        var R2 = parametro.getCor()[3];
        var G2 = parametro.getCor()[4];
        var B2 = parametro.getCor()[5];
        var R3 = parametro.getCor()[3];
        var G3 = parametro.getCor()[4];
        var B3 = parametro.getCor()[5];
        var RC = parametro.getCorrecao()[0];
        var GC = parametro.getCorrecao()[1];
        var BC = parametro.getCorrecao()[2];
        if (parametroDispositivo.isPresent() && !parametroDispositivo.get().getConfiguracao().getTipoCor().equals(TipoCor.RGB)) {
            if (parametroDispositivo.get().getConfiguracao().getTipoCor().equals(TipoCor.RBG)) {
                parametro.setCor(new int[]{R1, B1, G1, R2, B2, G2, R3, B3, G3});
                parametro.setCorrecao(new int[]{RC, BC, GC});
            } else if (parametroDispositivo.get().getConfiguracao().getTipoCor().equals(TipoCor.BRG)) {
                parametro.setCor(new int[]{B1, R1, G1, B2, R2, G2, B3, R3, G3});
                parametro.setCorrecao(new int[]{BC, RC, GC});
            } else if (parametroDispositivo.get().getConfiguracao().getTipoCor().equals(TipoCor.BGR)) {
                parametro.setCor(new int[]{B1, G1, R1, B2, G2, R2, B3, G3, R3});
                parametro.setCorrecao(new int[]{BC, GC, RC});
            } else if (parametroDispositivo.get().getConfiguracao().getTipoCor().equals(TipoCor.GBR)) {
                parametro.setCor(new int[]{G1, B1, R1, G2, B2, R2, G3, B3, R3});
                parametro.setCorrecao(new int[]{GC, BC, RC});
            } else if (parametroDispositivo.get().getConfiguracao().getTipoCor().equals(TipoCor.GRB)) {
                parametro.setCor(new int[]{G1, R1, B1, G2, R2, B2, G3, R3, B3});
                parametro.setCorrecao(new int[]{GC, RC, BC});
            }

        }
    }

    private static String toHexa(int value) {
        return String.format("%02X", value);
    }

    public static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return data;
    }

    private static String toHexa(int value, boolean forceQuatroCasas) {
        return String.format("%04X", value);
    }
}
