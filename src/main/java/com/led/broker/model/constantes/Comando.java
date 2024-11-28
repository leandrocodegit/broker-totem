package com.led.broker.model.constantes;

public enum Comando {

    ACEITO("Comando aceito pelo dispositivo"),
    ENVIADO("Comando enviado"),
    ENVIAR("Comando pendente"),
    SISTEMA("Comando enviado pelo sistema"),
    NENHUM_DEVICE("Comando não enviado, dispositivo inválido"),
    ONLINE("Dispositivo %s online"),
    OFFLINE("Dispositivo %s offline"),
    SINCRONIZAR("Sincronização enviada"),
    CONFIGURACAO("Solicitação de paramentros configuração"),
    TESTE("Teste de cor"),
    CONCLUIDO("Teste concluido"),
    TIMER_CONCLUIDO("Timer finalizado para %s"),
    TIMER_CRIADO("Timer criado para %s"),
    UPDATE("Comando de atualização de firmware enviado para %s"),
    FIRMWARE("Firmware atualizado para dispositivo %S"),
    PARAMETRO("Parametrizando cores");

    private String value;

    Comando(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
