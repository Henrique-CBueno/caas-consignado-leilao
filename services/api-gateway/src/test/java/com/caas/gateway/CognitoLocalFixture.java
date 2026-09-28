package com.caas.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

// Pool + client no cognito-local; cria usuários (com ou sem o atributo de tenant) e devolve o ID token,
// que é o token que carrega custom:tenant_id (o access token não carrega).
final class CognitoLocalFixture {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final String poolId;
    private final String clientId;

    CognitoLocalFixture(String baseUrl) throws IOException, InterruptedException {
        this.baseUrl = baseUrl;
        this.poolId = call("CreateUserPool", Map.of(
            "PoolName", "test-pool",
            "Schema", List.of(Map.of("Name", "tenant_id", "AttributeDataType", "String", "Mutable", true))
        )).path("UserPool").path("Id").asText();
        this.clientId = call("CreateUserPoolClient", Map.of(
            "UserPoolId", poolId,
            "ClientName", "test-client",
            "ExplicitAuthFlows", List.of("ALLOW_ADMIN_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH")
        )).path("UserPoolClient").path("ClientId").asText();
    }

    String jwkSetUri() {
        return baseUrl + "/" + poolId + "/.well-known/jwks.json";
    }

    // tenantClaim == null cria o usuário sem o atributo customizado.
    String idTokenFor(String username, String tenantClaim) throws IOException, InterruptedException {
        List<Map<String, String>> attributes = tenantClaim == null
            ? List.of()
            : List.of(Map.of("Name", "custom:tenant_id", "Value", tenantClaim));
        call("AdminCreateUser", Map.of(
            "UserPoolId", poolId, "Username", username, "TemporaryPassword", "Temp1234!",
            "MessageAction", "SUPPRESS", "UserAttributes", attributes
        ));
        call("AdminSetUserPassword", Map.of(
            "UserPoolId", poolId, "Username", username, "Password", "Passw0rd1!", "Permanent", true
        ));
        return call("AdminInitiateAuth", Map.of(
            "UserPoolId", poolId, "ClientId", clientId, "AuthFlow", "ADMIN_USER_PASSWORD_AUTH",
            "AuthParameters", Map.of("USERNAME", username, "PASSWORD", "Passw0rd1!")
        )).path("AuthenticationResult").path("IdToken").asText();
    }

    private JsonNode call(String target, Map<String, Object> body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
            .header("Content-Type", "application/x-amz-json-1.1")
            .header("X-Amz-Target", "AWSCognitoIdentityProviderService." + target)
            .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
            .build();
        return MAPPER.readTree(HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body());
    }
}
