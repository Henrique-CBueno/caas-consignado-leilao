package com.caas.tenant.infrastructure.persistence

import com.caas.tenant.application.TenantRepository
import com.caas.tenant.domain.Tenant
import com.caas.tenant.domain.TenantId
import com.caas.tenant.infrastructure.TenantContextHolder
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Entity
@Table(name = "tenants")
class TenantJpaEntity(
    @Id
    val id: UUID,
    val name: String,
) {
    protected constructor() : this(UUID(0, 0), "")
}

interface SpringDataTenantRepository : JpaRepository<TenantJpaEntity, UUID>

@Repository
class JpaTenantRepository(
    private val springDataRepository: SpringDataTenantRepository,
    private val entityManager: EntityManager,
) : TenantRepository {

    @Transactional
    override fun save(tenant: Tenant): Tenant {
        applyTenantContext()
        val saved = springDataRepository.save(TenantJpaEntity(tenant.id.value, tenant.name))
        return Tenant(TenantId(saved.id), saved.name)
    }

    @Transactional
    override fun findById(id: TenantId): Tenant? {
        applyTenantContext()
        return springDataRepository.findById(id.value)
            .map { Tenant(TenantId(it.id), it.name) }
            .orElse(null)
    }

    // set_config(..., true) == SET LOCAL: escopo à transação atual, seguro com
    // connection pooling (HikariCP não garante a mesma conexão entre chamadas).
    //
    // "SET LOCAL ROLE app_role" é obrigatório: o login role do Testcontainers/RDS
    // emulado é superusuário (dono da tabela), e superusuário sempre ignora RLS,
    // mesmo com FORCE ROW LEVEL SECURITY — só uma role restrita respeita a policy.
    private fun applyTenantContext() {
        entityManager.createNativeQuery("SET LOCAL ROLE app_role").executeUpdate()
        val tenantId = TenantContextHolder.get() ?: return
        entityManager
            .createNativeQuery("SELECT set_config('app.current_tenant', :tenantId, true)")
            .setParameter("tenantId", tenantId.value.toString())
            .singleResult
    }
}
