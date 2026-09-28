package com.caas.gateway.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

// Fala com o cognito-local pelo protocolo JSON do Cognito, só pelo fluxo PÚBLICO de login
// (InitiateAuth — que, ao contrário de AdminInitiateAuth, não exige UserPoolId nem credencial
// de administrador, só o ClientId). O ClientId vem de configuração (o mesmo processo que já
// escreve app.cognito.jwk-set-uri no provisionamento do cluster escreve este valor também).
@Component
public class CognitoAuthClient {

    private static final String TARGET_HEADER = "X-Amz-Target";
    private static final String TARGET = "AWSCognitoIdentityProviderService.InitiateAuth";
    // cognito-local exige exatamente este Content-Type para aceitar o corpo (application/json
    // é rejeitado com 500); o WebClient não tem um encoder para ele, então o corpo é serializado
    // à mão em bytes, contornando a checagem de encoder por mimetype.
    private static final MediaType AWS_JSON = MediaType.valueOf("application/x-amz-json-1.1");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String clientId;

    public CognitoAuthClient(
        WebClient.Builder builder,
        ObjectMapper objectMapper,
        @Value("${app.cognito.base-uri}") String baseUri,
        @Value("${app.cognito.client-id}") String clientId
    ) {
        this.webClient = builder.baseUrl(baseUri).build();
        this.objectMapper = objectMapper;
        this.clientId = clientId;
    }

    public Mono<String> login(String username, String password) {
        Map<String, Object> body = Map.of(
            "ClientId", clientId,
            "AuthFlow", "USER_PASSWORD_AUTH",
            "AuthParameters", Map.of("USERNAME", username, "PASSWORD", password)
        );
        return webClient.post()
            .header(TARGET_HEADER, TARGET)
            .contentType(AWS_JSON)
            .bodyValue(toJsonBytes(body))
            .retrieve()
            .bodyToMono(String.class)
            .map(this::parseIdToken);
    }

    private byte[] toJsonBytes(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String parseIdToken(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.path("AuthenticationResult").path("IdToken").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Resposta inválida do cognito-local", e);
        }
    }
}
