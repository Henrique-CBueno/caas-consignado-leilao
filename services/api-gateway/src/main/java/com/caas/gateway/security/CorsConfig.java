package com.caas.gateway.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

// CORS liberado só nas rotas que o front chama do navegador (login e criar proposta); o resto da
// borda pública não precisa (o dashboard fala com elas via <img>/redirect, nunca fetch/XHR).
// Só a origem do dashboard (APP_CORS_ALLOWED_ORIGINS) passa; a proteção real continua sendo o
// JWT exigido depois do preflight (Milestone 18 substituiu o antigo "*" da Milestone 16).
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
        @Value("${app.cors.allowed-origins}") List<String> allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/auth/**", configuration);
        source.registerCorsConfiguration("/proposals/**", configuration);
        source.registerCorsConfiguration("/admin/**", configuration);
        return source;
    }
}
