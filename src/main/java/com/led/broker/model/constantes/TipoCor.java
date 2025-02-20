package com.led.broker.model.constantes;

public enum TipoCor {

    RGB(0),
    RBG(1),
    BGR(2),
    BRG(3),
    GRB(4),
    GBR(5);

    public int codigo;

    TipoCor(int codigo) {
        this.codigo = codigo;
    }
}
