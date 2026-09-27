package com.caas.credit.infrastructure.vault;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

// Contexto minimo (sem datasource/Kafka): o seam sob teste eh so a resolucao de
// propriedade via Vault real, nao o app inteiro.
//
// spring.config.import=vault:// eh resolvido no ConfigDataEnvironmentPostProcessor,
// uma fase MAIS CEDO do bootstrap do Spring Boot do que quando @DynamicPropertySource
// consegue injetar valores (via ApplicationContextInitializer). Por isso a URI/token
// do container precisam virar system properties reais (SystemPropertiesPropertySource
// existe desde o inicio), nao @DynamicPropertySource — descoberto empiricamente
// (a primeira tentativa com @DynamicPropertySource falhou com "Cannot create
// authentication mechanism for TOKEN").
@SpringBootTest(
    classes = VaultSecretIntegrationTest.MinimalConfig.class,
    properties = {"spring.config.import=vault://", "spring.cloud.vault.enabled=true"}
)
class VaultSecretIntegrationTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        KafkaAutoConfiguration.class
    })
    static class MinimalConfig {
    }

    private static final VaultContainer<?> VAULT =
        new VaultContainer<>(DockerImageName.parse("hashicorp/vault:1.18"))
            .withVaultToken("test-token")
            .withSecretInVault("secret/credit-analysis-service", "app.jev.api-key=test-secret-value");

    static {
        VAULT.start();
        System.setProperty("spring.cloud.vault.uri", "http://" + VAULT.getHost() + ":" + VAULT.getMappedPort(8200));
        System.setProperty("spring.cloud.vault.token", "test-token");
    }

    @AfterAll
    static void stopVault() {
        VAULT.stop();
        System.clearProperty("spring.cloud.vault.uri");
        System.clearProperty("spring.cloud.vault.token");
    }

    @Value("${app.jev.api-key}")
    private String apiKey;

    @Test
    void readsTheOpenRouterApiKeyFromVault() {
        assertThat(apiKey).isEqualTo("test-secret-value");
    }
}
