package com.led.broker.model.constantes;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Comando {

    ONLINE(0,"Dispositivo %s online"),
    CONFIGURACAO(1,"Solicitação de paramentros configuração"),
    CONCLUIDO(2,"Teste concluido"),
    ACEITO(3, "Comando aceito pelo dispositivo"),
    FIRMWARE(4,"Firmware atualizado para dispositivo %S"),
    ENVIADO(5,"Comando enviado"),
    ENVIAR(6,"Comando pendente"),
    SISTEMA(7,"Comando enviado pelo sistema"),
    NENHUM_DEVICE(8,"Comando não enviado, dispositivo inválido"),
    OFFLINE(9,"Dispositivo %s offline"),
    SINCRONIZAR(10,"Sincronização enviada"),
    TESTE(11,"Teste de cor"),
    TIMER_CONCLUIDO(12,"Timer finalizado para %s"),
    TIMER_CRIADO(13,"Timer criado para %s"),
    UPDATE(14, "Comando de atualização de firmware enviado para %s"),
    PARAMETRO(15,"Parametrizando cores");

    public String value;
    public int codigo;


    Comando( int codigo,String value) {
        this.value = value;
        this.codigo = codigo;
    }

    @JsonCreator
    public static Comando fromDescricao(int codigo) {
        for (Comando tipo : values()) {
            if (tipo.codigo == codigo) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Comando inválido: " + codigo);
    }


}
