package com.led.broker.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ParamsLora {

    private String payload;
    private String encrypted_payload;
    private Hardware hardware;
    private String code;
}



