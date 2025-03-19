package com.led.broker.handler;

import com.led.broker.model.Mensagem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class LoRaDecrypt {


    public String decript(Mensagem mensagem) {
        try {

            if(mensagem.getParams().getEncrypted_payload() == null)
                return null;


            byte[] decodedBytes = Base64.getDecoder().decode(mensagem.getParams().getPayload());
            String decodedString = new String(decodedBytes);


            return decodedString;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
