package com.led.broker.model.constantes;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoConfiguracao {

    LED(1),
    LED_RESTART(2),
    VIBRACAO(3),
    WIFI(4),
    LORA_WAN(5),
    LIMPAR_FLASH(6),
    A(7),
    B(8),
    C(9),
    UPDATE(10);

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