package com.caas.tenant.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// Milestone 17: API administrativa de tenants. Postgres e cognito-local reais.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AdminTenantControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> cognitoLocal =
        new GenericContainer<>(DockerImageName.parse("jagregory/cognito-local:latest"))
            .withExposedPorts(9229)
            .waitingFor(Wait.forLogMessage(".*Cognito Local running.*\\n", 1));

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String cognitoUrl;
    private static String clientId;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        cognitoUrl = "http://" + cognitoLocal.getHost() + ":" + cognitoLocal.getMappedPort(9229);
        String poolId = cognito("CreateUserPool", Map.of("PoolName", "test-pool", "Schema", List.of(
            Map.of("Name", "tenant_id", "AttributeDataType", "String", "Mutable", true)))).path("UserPool").path("Id").asText();
        clientId = cognito("CreateUserPoolClient", Map.of("UserPoolId", poolId, "ClientName", "c",
            "ExplicitAuthFlows", List.of("ALLOW_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"))).path("UserPoolClient").path("ClientId").asText();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.cognito.base-uri", () -> cognitoUrl);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private com.caas.tenant.application.AdminTenantRepository adminRepository;

    private ResponseEntity<String> create(String name) {
        return restTemplate.postForEntity(url(), Map.of("name", name), String.class);
    }

    private String url() {
        return "http://localhost:" + port + "/admin/tenants";
    }

    @Test
    void creatingATenantListsItAndLetsItsDemoUserLogIn() throws Exception {
        ResponseEntity<String> created = create("Banco Ômega");

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String id = MAPPER.readTree(created.getBody()).path("id").asText();
        assertThat(restTemplate.getForObject(url(), String.class)).contains(id, "Banco Ômega", "Banco Alfa");

        JsonNode auth = cognito("InitiateAuth", Map.of("ClientId", clientId, "AuthFlow", "USER_PASSWORD_AUTH",
            "AuthParameters", Map.of("USERNAME", "banco-omega@caas.local", "PASSWORD", "Passw0rd1!")));
        String idToken = auth.path("AuthenticationResult").path("IdToken").asText();
        JsonNode claims = MAPPER.readTree(Base64.getUrlDecoder().decode(idToken.split("\\.")[1]));
        assertThat(claims.path("custom:tenant_id").asText()).isEqualTo(id);
    }

    @Test
    void anEmptyNameIsRejected() {
        assertThat(create("  ").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aDuplicateNameIsRejected() {
        create("Banco Repetido");

        assertThat(create("Banco Repetido").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // Tenant que ficou gravado sem usuário demo (provedor de identidade caiu na criação): repetir repara.
    @Test
    void repeatingTheCreationOfATenantWithoutADemoUserCompletesIt() throws Exception {
        String id = adminRepository.create("Banco Orfao").id().value().toString();

        ResponseEntity<String> repaired = create("Banco Orfao");

        assertThat(repaired.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(repaired.getBody()).contains(id);
        JsonNode auth = cognito("InitiateAuth", Map.of("ClientId", clientId, "AuthFlow", "USER_PASSWORD_AUTH",
            "AuthParameters", Map.of("USERNAME", "banco-orfao@caas.local", "PASSWORD", "Passw0rd1!")));
        assertThat(auth.path("AuthenticationResult").path("IdToken").asText()).isNotBlank();
        assertThat(create("Banco Orfao").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private static JsonNode cognito(String target, Map<String, Object> body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(cognitoUrl))
            .header("Content-Type", "application/x-amz-json-1.1")
            .header("X-Amz-Target", "AWSCognitoIdentityProviderService." + target)
            .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body))).build();
        return MAPPER.readTree(HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body());
    }
}
