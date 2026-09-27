package com.caas.tenant.infrastructure.web

import com.caas.tenant.application.TenantRepository
import com.caas.tenant.domain.Tenant
import com.caas.tenant.domain.TenantId
import com.caas.tenant.infrastructure.TenantContextHolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TenantControllerIntegrationTest {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var tenantRepository: TenantRepository

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clear()
    }

    private fun seed(tenant: Tenant) {
        TenantContextHolder.set(tenant.id)
        tenantRepository.save(tenant)
        TenantContextHolder.clear()
    }

    private fun getTenant(
        path: UUID,
        callerTenantId: UUID,
    ) = restTemplate.exchange(
        "http://localhost:$port/tenants/$path",
        HttpMethod.GET,
        HttpEntity<Void>(HttpHeaders().apply { set("X-Tenant-Id", callerTenantId.toString()) }),
        String::class.java,
    )

    @Test
    fun `an authenticated tenant can retrieve its own config via the API`() {
        val tenantA = Tenant(TenantId(UUID.randomUUID()), "Banco Alfa")
        seed(tenantA)

        val response = getTenant(tenantA.id.value, tenantA.id.value)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("Banco Alfa")
    }

    @Test
    fun `a tenant cannot retrieve another tenant's config via the API`() {
        val tenantA = Tenant(TenantId(UUID.randomUUID()), "Banco Alfa")
        val tenantB = Tenant(TenantId(UUID.randomUUID()), "Banco Beta")
        seed(tenantA)
        seed(tenantB)

        val response = getTenant(tenantB.id.value, tenantA.id.value)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
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
