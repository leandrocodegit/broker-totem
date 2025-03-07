package com.led.broker.model.constantes;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ClasseLora {

    A(0),
    C(1);

    public int codigo;

    ClasseLora(int codigo) {
        this.codigo = codigo;
    }

    @JsonCreator
    public static ClasseLora fromCodigo(int codigo) {
        for (ClasseLora tipo : values()) {
            if (tipo.codigo == codigo) {
                return tipo;
            }
        }
        return A;
    }
}
