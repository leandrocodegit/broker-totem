package com.led.broker.util;

import com.led.broker.controller.response.DispositivoDashResponse;
import com.led.broker.controller.response.DispositivoResponse;
import com.led.broker.model.Dispositivo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TimeUtil {

    private TimeUtil(){}

    public static Map<String, Dispositivo> timers = new HashMap<>();
    public static boolean isTime(Dispositivo dispositivo) {
        if (dispositivo == null || dispositivo.getTemporizador() == null) {
            return false;
        }
        long differenceInMinutes = Duration.between(dispositivo.getTemporizador().getTime(), LocalDateTime.now()).toMinutes();
        return differenceInMinutes <= 0;
    }

    public static boolean isTime(DispositivoResponse dispositivo) {
        if (dispositivo == null || dispositivo.getTemporizador() == null) {
            return false;
        }
        long differenceInMinutes = Duration.between(dispositivo.getTemporizador().getTime(), LocalDateTime.now()).toMinutes();
        return differenceInMinutes <= 0;
    }

//    public static boolean isTime(DispositivoDashResponse dispositivo) {
//        if (dispositivo == null || dispositivo.getTemporizador() == null) {
//            return false;
//        }
//        long differenceInMinutes = Duration.between(dispositivo.getTemporizador().getTime(), LocalDateTime.now()).toMinutes();
//        return differenceInMinutes <= 0;
//    }
}
