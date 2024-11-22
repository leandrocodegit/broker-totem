package com.led.broker.service;


import com.led.broker.controller.response.DashboardResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DashboardService {
    private final RestTemplate restTemplate;
    private final String url = "http://vps55601.publiccloud.com.br:8000/totem";

    public DashboardService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public DashboardResponse atualizarDashboard(String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<DashboardResponse> response = restTemplate.exchange(url + "/dashboard/gerar", HttpMethod.GET, entity, DashboardResponse.class);
        return response.getBody();
    }

}