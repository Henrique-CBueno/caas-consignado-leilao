package com.caas.gateway.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

// CORS liberado só nas rotas que o front chama do navegador (login e criar proposta); o resto da
// borda pública não precisa (o dashboard fala com elas via <img>/redirect, nunca fetch/XHR).
// Origem "*" é uma limitação assumida de ambiente de demonstração (Milestone 16): a proteção real
// continua sendo o JWT exigido depois do preflight, nunca a origem.
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/auth/**", configuration);
        source.registerCorsConfiguration("/proposals/**", configuration);
        source.registerCorsConfiguration("/admin/**", configuration);
        return source;
    }
}
