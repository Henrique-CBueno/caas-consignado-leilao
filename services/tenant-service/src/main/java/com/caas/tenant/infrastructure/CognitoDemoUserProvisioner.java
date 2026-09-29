package com.caas.tenant.infrastructure;

import com.caas.tenant.application.DemoUserProvisioner;
import com.caas.tenant.domain.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Mesmo protocolo do bootstrap do cluster. O emulador exige o content-type do protocolo AWS
// (com application/json responde 500). Senha fixa de demonstração: ADR-0017, não é segredo real.
@Component
public class CognitoDemoUserProvisioner implements DemoUserProvisioner {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private final String baseUri;
    private final String demoPassword;

    public CognitoDemoUserProvisioner(
        @Value("${app.cognito.base-uri}") String baseUri,
        @Value("${app.auth.demo-password}") String demoPassword
    ) {
        this.baseUri = baseUri;
        this.demoPassword = demoPassword;
    }

    // O emulador tem um único pool ("caas"): descobre o id em vez de exigir mais configuração.
    @Override
    public void provision(Tenant tenant) {
        try {
            String poolId = call("ListUserPools", Map.of("MaxResults", 10)).path("UserPools").path(0).path("Id").asText();
            if (poolId.isEmpty()) {
                throw new IllegalStateException("nenhum user pool no provedor de identidade");
            }
            String username = usernameFor(tenant.name());
            JsonNode created = call("AdminCreateUser", Map.of(
                "UserPoolId", poolId, "Username", username, "TemporaryPassword", "Temp1234!",
                "MessageAction", "SUPPRESS",
                "UserAttributes", List.of(Map.of("Name", "custom:tenant_id", "Value", tenant.id().value().toString()))));
            if (created.has("__type")) {
                throw new IllegalStateException("AdminCreateUser falhou: " + created.path("__type").asText());
            }
            call("AdminSetUserPassword", Map.of(
                "UserPoolId", poolId, "Username", username, "Password", demoPassword, "Permanent", true));
        } catch (IOException e) {
            throw new IllegalStateException("provedor de identidade inacessível", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrompido", e);
        }
    }

    public static String usernameFor(String tenantName) {
        String slug = Normalizer.normalize(tenantName, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return slug + "@caas.local";
    }

    private JsonNode call(String target, Map<String, Object> body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUri))
            .header("Content-Type", "application/x-amz-json-1.1")
            .header("X-Amz-Target", "AWSCognitoIdentityProviderService." + target)
            .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body))).build();
        return MAPPER.readTree(HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body());
    }
}
