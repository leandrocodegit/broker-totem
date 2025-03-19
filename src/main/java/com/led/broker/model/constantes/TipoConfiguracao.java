package com.led.broker.model.constantes;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoConfiguracao {

    LED(1),
    LED_RESTART(2),
    VIBRACAO(3),
    WIFI(4),
    LORA_WAN(5),
    LORA_WAN_PARAM(6),
    LORA_WAN_JOIN(7),
    LORA_WAN_SEND(8),
    LIMPAR_FLASH(9),
    LORA_WAN_RESET(10),
    ID(11),
    DEBUG(12),

    UPDATE(13);

    public int codigo;

    TipoConfiguracao(int codigo) {
        this.codigo = codigo;
    }

    @JsonCreator
    public static TipoConfiguracao fromDescricao(int codigo) {
        for (TipoConfiguracao tipo : values()) {
            if (tipo.codigo == codigo) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Codigo inválido: " + codigo);
    }
}