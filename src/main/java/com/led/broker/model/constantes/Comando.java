package com.led.broker.model.constantes;

public enum Comando {

    ACEITO("Comando aceito pelo dispositivo"),
    ENVIADO("Comando enviado"),
    ENVIAR("Comando pendente"),
    SISTEMA("Comando enviado pelo sistema"),
    NENHUM_DEVICE("Comando não enviado, dispositivo inválido"),
    ONLINE("Dispositivo %S online"),
    OFFLINE("Dispositivo %S offline"),
    SINCRONIZAR("Sincronização enviada"),
    CONFIGURACAO("Solicitação de paramentros configuração"),
    TESTE("Teste de cor"),
    CONCLUIDO("Teste concluido"),
    TIMER_CONCLUIDO("Timer finalizado para %S"),
    TIMER_CRIADO("Timer criado para %S"),
    UPDATE("Comando de atualização de firmware enviado para %S"),
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
