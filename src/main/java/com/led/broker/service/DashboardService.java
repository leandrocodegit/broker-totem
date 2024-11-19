package com.led.broker.service;


import com.led.broker.controller.response.DashboardResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

@Service
public interface DashboardService {

    @GetMapping
    public DashboardResponse gerarDashBoard();

}