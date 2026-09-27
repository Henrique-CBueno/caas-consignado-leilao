package com.caas.gateway

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

// Cognito real via LocalStack não é viável: o LocalStack Community não implementa
// cognito-idp de forma alguma (confirmado empiricamente na Milestone 1 — ver ADR-0026).
// cognito-local (jagregory/cognito-local) é usado aqui: emulador dedicado, gratuito,
// que assina JWTs de verdade com JWKS real via a mesma API JSON do Cognito.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GatewayValidJwtRoutingIntegrationTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `a request with a valid Cognito-issued token is routed through`() {
        webTestClient.get().uri("/tenants/00000000-0000-0000-0000-000000000000")
            .header("Authorization", "Bearer $idToken")
            .exchange()
            .expectStatus().isOk
    }

    companion object {
        @Container
        @JvmStatic
        val cognitoLocal =
            GenericContainer(DockerImageName.parse("jagregory/cognito-local:latest"))
                .withExposedPorts(9229)
                .waitingFor(Wait.forLogMessage(".*Cognito Local running.*\\n", 1))

        private val downstream: HttpServer = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/tenants") { exchange ->
                val body = "ok".toByteArray()
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }
        private val httpClient = HttpClient.newHttpClient()
        private val objectMapper = ObjectMapper()

        lateinit var idToken: String

        @AfterAll
        @JvmStatic
        fun tearDown() {
            downstream.stop(0)
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            val baseUrl = "http://${cognitoLocal.host}:${cognitoLocal.getMappedPort(9229)}"

            val poolId =
                cognito(baseUrl, "CreateUserPool", mapOf("PoolName" to "test-pool"))
                    .path("UserPool").path("Id").asText()

            val clientId =
                cognito(
                    baseUrl,
                    "CreateUserPoolClient",
                    mapOf(
                        "UserPoolId" to poolId,
                        "ClientName" to "test-client",
                        "ExplicitAuthFlows" to listOf("ALLOW_ADMIN_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"),
                    ),
                ).path("UserPoolClient").path("ClientId").asText()

            cognito(
                baseUrl,
                "AdminCreateUser",
                mapOf(
                    "UserPoolId" to poolId,
                    "Username" to "tenant-a@example.com",
                    "TemporaryPassword" to "Temp1234!",
                    "MessageAction" to "SUPPRESS",
                ),
            )
            cognito(
                baseUrl,
                "AdminSetUserPassword",
                mapOf(
                    "UserPoolId" to poolId,
                    "Username" to "tenant-a@example.com",
                    "Password" to "Passw0rd1!",
                    "Permanent" to true,
                ),
            )
            val auth =
                cognito(
                    baseUrl,
                    "AdminInitiateAuth",
                    mapOf(
                        "UserPoolId" to poolId,
                        "ClientId" to clientId,
                        "AuthFlow" to "ADMIN_USER_PASSWORD_AUTH",
                        "AuthParameters" to mapOf("USERNAME" to "tenant-a@example.com", "PASSWORD" to "Passw0rd1!"),
                    ),
                )
            idToken = auth.path("AuthenticationResult").path("IdToken").asText()

            registry.add("app.cognito.jwk-set-uri") { "$baseUrl/$poolId/.well-known/jwks.json" }
            registry.add("app.routes.tenant-service-uri") { "http://localhost:${downstream.address.port}" }
        }

        private fun cognito(
            baseUrl: String,
            target: String,
            body: Map<String, Any>,
        ): JsonNode {
            val request =
                HttpRequest.newBuilder(URI.create(baseUrl))
                    .header("Content-Type", "application/x-amz-json-1.1")
                    .header("X-Amz-Target", "AWSCognitoIdentityProviderService.$target")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            return objectMapper.readTree(response.body())
        }
    }
}
