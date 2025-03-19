package com.led.broker.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KoreMensagem {

    private String devEUI;
    private String payload;
    private int port;

    @Override
    public String toString() {
        return "{" +
                "devEUI='" + devEUI + '\'' +
                ", payload='" + payload + '\'' +
                ", port=" + port +
                '}';
    }
}
