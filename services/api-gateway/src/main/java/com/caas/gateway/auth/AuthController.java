package com.caas.gateway.auth;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

// Login com um dos tenants de demonstração (senha fixa, já documentada — ADR-0017); nunca exige
// credencial de administrador do Cognito. Único endpoint público além de saúde/métricas (Milestone 16).
@RestController
public class AuthController {

    private static final Set<String> DEMO_TENANTS = Set.of("alfa", "beta", "gama");

    private final CognitoAuthClient cognitoAuthClient;
    private final String demoPassword;

    public AuthController(
        CognitoAuthClient cognitoAuthClient,
        @Value("${app.auth.demo-password}") String demoPassword
    ) {
        this.cognitoAuthClient = cognitoAuthClient;
        this.demoPassword = demoPassword;
    }

    @PostMapping("/auth/login")
    @ResponseStatus(HttpStatus.OK)
    public Mono<LoginResponse> login(@RequestBody LoginRequest request) {
        if (!DEMO_TENANTS.contains(request.tenant())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant de demonstração desconhecido");
        }
        String username = request.tenant() + "@caas.local";
        return cognitoAuthClient.login(username, demoPassword).map(LoginResponse::new);
    }
}
