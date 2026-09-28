package com.caas.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Destino do CircuitBreaker de rota (ver GatewayRoutesConfig): resposta rápida e
// previsível quando o circuito está aberto, em vez de deixar o timeout/erro de
// conexão cru do downstream vazar para quem chamou.
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public ResponseEntity<String> fallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Serviço temporariamente indisponível");
    }
}
