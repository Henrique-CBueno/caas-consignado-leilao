package com.caas.tenant.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.caas.tenant.application.AdminTenantRepository;
import com.caas.tenant.application.TenantRepository;
import com.caas.tenant.domain.Tenant;
import com.caas.tenant.domain.TenantId;
import com.caas.tenant.infrastructure.TenantContextHolder;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class AdminTenantRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AdminTenantRepository adminRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private DataSource dataSource;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void adminListsTenantsOfAllTenantsAndCreatedOnesAppear() {
        Tenant created = adminRepository.create("Banco Delta");

        assertThat(adminRepository.list()).extracting(Tenant::name)
            .contains("Banco Alfa", "Banco Beta", "Fintech Gama", "Banco Delta");
        assertThat(created.id().value()).isNotNull();
    }

    @Test
    void adminCannotCreateTwoTenantsWithTheSameName() {
        adminRepository.create("Banco Duplicado");

        assertThatThrownBy(() -> adminRepository.create("Banco Duplicado")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void aTenantSessionStillSeesOnlyItsOwnRow() {
        Tenant created = adminRepository.create("Banco Isolado");
        TenantContextHolder.set(created.id());

        assertThat(tenantRepository.findById(created.id())).isNotNull();
        assertThat(tenantRepository.findById(new TenantId(UUID.fromString("11111111-1111-1111-1111-111111111111"))))
            .isNull();
    }

    @Test
    void adminRoleCannotUpdateOrDeleteTenants() {
        new JdbcTemplate(dataSource).execute((java.sql.Connection c) -> {
            try (var st = c.createStatement()) {
                st.execute("SET ROLE admin_role");
                assertThatThrownBy(() -> st.executeUpdate("UPDATE tenants SET name = 'x'"))
                    .hasMessageContaining("permission denied");
                assertThatThrownBy(() -> st.executeUpdate("DELETE FROM tenants"))
                    .hasMessageContaining("permission denied");
                st.execute("RESET ROLE");
            }
            return null;
        });
    }
}
