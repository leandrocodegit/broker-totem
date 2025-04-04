package com.led.broker.util;

import com.led.broker.model.Dispositivo;
import com.led.broker.model.constantes.ModoOperacao;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TimeUtil {

    private TimeUtil(){}

    public static Map<Long, Dispositivo> timers = new HashMap<>();
    public static boolean isTime(Dispositivo dispositivo) {
        if (dispositivo == null || dispositivo.getOperacao() == null || !dispositivo.getOperacao().getModoOperacao().equals(ModoOperacao.TEMPORIZADOR)) {
            return false;
        }
        long differenceInMinutes = Duration.between(dispositivo.getOperacao().getTime(), LocalDateTime.now()).toMinutes();
        return differenceInMinutes <= 0;
    }
}
