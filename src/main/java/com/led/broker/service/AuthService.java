package com.led.broker.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {


    private final RestTemplate restTemplate;
    private final String url = "http://vps55601.publiccloud.com.br:8000/totem";

    public AuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public boolean validarToken(String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Boolean> response = restTemplate.exchange(url + "/auth/valid", HttpMethod.GET, entity, Boolean.class);
        return response.getBody();
    }

    public boolean validarwebSocker(String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Boolean> response = restTemplate.exchange(url + "/auth/ws", HttpMethod.GET, entity, Boolean.class);
        return response.getBody();
    }

}
