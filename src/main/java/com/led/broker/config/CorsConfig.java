package com.led.broker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Collections;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter  corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedHeader("*"); // Permitir todos os cabeçalhos
        corsConfig.addAllowedMethod("*"); // Permitir todos os métodos
        corsConfig.setAllowCredentials(false); // Não permitir credenciais (cookies, headers de autorização, etc.)

        // Não adicionar nenhuma origem
        corsConfig.setAllowedOrigins(Collections.emptyList()); // Nenhuma origem permitida

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig); // Aplicar a configuração para todos os endpoints

        return new CorsWebFilter(source);
    }
}