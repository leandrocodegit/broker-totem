package com.led.broker.controller;

import com.led.broker.model.constantes.Topico;
import com.led.broker.service.DashboardService;
import com.led.broker.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("conexao")
@RequiredArgsConstructor
public class ConexaoController {

    private final DashboardService dashboardService;
    private final MqttService mqttService;

    @GetMapping
    public void atualizarDashboar(){
        dashboardService.atualizarDashboard("", true);
        mqttService.sendRetainedMessage(Topico.TOPICO_DASHBOARD, "Atualizando dashboard");
    }
}
