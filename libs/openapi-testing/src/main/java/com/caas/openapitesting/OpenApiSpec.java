package com.caas.openapitesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.test.web.servlet.MockMvc;

public final class OpenApiSpec {

    // Com -Dopenapi.update=true (repassado pelo Gradle) o arquivo é reescrito em vez de comparado.
    private static final boolean UPDATE = Boolean.getBoolean("openapi.update");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OpenApiSpec() {
    }

    public static void assertMatchesCommitted(MockMvc mockMvc, String serviceName) throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        ObjectNode generated = (ObjectNode) MAPPER.readTree(body);
        // "servers" traz o host do MockMvc (http://localhost), que não faz parte do contrato.
        generated.remove("servers");

        Path committed = Path.of("../../docs/openapi/" + serviceName + ".json");
        if (UPDATE) {
            Files.createDirectories(committed.getParent());
            Files.writeString(committed, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(generated) + "\n");
            return;
        }
        assertThat(committed)
            .as("OpenAPI de %s não commitada; gere com: ./gradlew contractTest -Dopenapi.update=true", serviceName)
            .exists();
        assertThat(MAPPER.readTree(committed.toFile()))
            .as("OpenAPI de %s divergiu do código; se a mudança é intencional: ./gradlew contractTest -Dopenapi.update=true", serviceName)
            .isEqualTo(generated);
    }
}
