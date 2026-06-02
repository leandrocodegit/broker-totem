package com.led.broker.util;

import com.led.broker.integracao.model.Dispositivo;
import com.led.broker.model.DispositivoEntity;
import com.led.broker.model.TemporizadorControle;
import com.led.broker.model.constantes.ModoOperacao;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TimeUtil {

    private TimeUtil(){}

    public static Map<Long, TemporizadorControle> timers = new HashMap<>();
    public static boolean isTime(Dispositivo dispositivoEntity) {
        if (dispositivoEntity == null || dispositivoEntity.getOperacao() == null || !dispositivoEntity.getOperacao().getModoOperacao().equals(ModoOperacao.TEMPORIZADOR)) {
            return false;
        }
        long differenceInMinutes = Duration.between(dispositivoEntity.getOperacao().getTime(), LocalDateTime.now()).toMinutes();
        return differenceInMinutes <= 0;
    }

    public static boolean isTimeTemporizador(TemporizadorControle temporizadorControle) {
        var dispositivo = temporizadorControle.getDispositivoEntity();
        if (dispositivo == null || dispositivo.getOperacao() == null || !dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.TEMPORIZADOR)) {
            return false;
        }
        return temporizadorControle.getTimeout().isBefore(LocalDateTime.now());
    }
}
