package com.caas.tenant.infrastructure.persistence

import com.caas.tenant.application.TenantRepository
import com.caas.tenant.domain.Tenant
import com.caas.tenant.domain.TenantId
import com.caas.tenant.infrastructure.TenantContextHolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest
@Testcontainers
class TenantRepositoryIntegrationTest {

    @Autowired
    lateinit var tenantRepository: TenantRepository

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clear()
    }

    @Test
    fun `a saved tenant can be retrieved by id`() {
        val tenant = Tenant(TenantId(UUID.randomUUID()), "Banco Alfa")

        TenantContextHolder.set(tenant.id)
        tenantRepository.save(tenant)
        val retrieved = tenantRepository.findById(tenant.id)

        assertThat(retrieved?.name).isEqualTo("Banco Alfa")
    }

    @Test
    fun `a tenant session cannot read another tenant's row by id`() {
        val tenantA = Tenant(TenantId(UUID.randomUUID()), "Banco Alfa")
        val tenantB = Tenant(TenantId(UUID.randomUUID()), "Banco Beta")

        TenantContextHolder.set(tenantA.id)
        tenantRepository.save(tenantA)

        TenantContextHolder.set(tenantB.id)
        tenantRepository.save(tenantB)

        val leaked = tenantRepository.findById(tenantA.id)

        assertThat(leaked).isNull()
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
