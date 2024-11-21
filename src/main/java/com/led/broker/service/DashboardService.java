package com.led.broker.service;


import com.led.broker.controller.response.DashboardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "dashboard", url = "http://192.168.10.2:8000/totem")
public interface DashboardService {
    @GetMapping("/dashboard/gerar")
    public DashboardResponse atualizarDashboard(@RequestHeader("Authorization") String authorization);

}