package com.caas.tenant.infrastructure.persistence;

import com.caas.tenant.application.TenantRepository;
import com.caas.tenant.domain.Tenant;
import com.caas.tenant.domain.TenantId;
import com.caas.tenant.infrastructure.TenantContextHolder;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaTenantRepository implements TenantRepository {

    private final SpringDataTenantRepository springDataRepository;
    private final EntityManager entityManager;

    public JpaTenantRepository(SpringDataTenantRepository springDataRepository, EntityManager entityManager) {
        this.springDataRepository = springDataRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Tenant save(Tenant tenant) {
        applyTenantContext();
        var saved = springDataRepository.save(new TenantJpaEntity(tenant.id().value(), tenant.name()));
        return toDomain(saved);
    }

    @Override
    @Transactional
    public Tenant findById(TenantId id) {
        applyTenantContext();
        return springDataRepository.findById(id.value()).map(JpaTenantRepository::toDomain).orElse(null);
    }

    // set_config(..., true) == SET LOCAL: escopo à transação atual, seguro com
    // connection pooling (HikariCP não garante a mesma conexão entre chamadas).
    //
    // "SET LOCAL ROLE app_role" é obrigatório: o login role do Testcontainers/RDS
    // emulado é superusuário (dono da tabela), e superusuário sempre ignora RLS,
    // mesmo com FORCE ROW LEVEL SECURITY — só uma role restrita respeita a policy.
    private void applyTenantContext() {
        entityManager.createNativeQuery("SET LOCAL ROLE app_role").executeUpdate();
        TenantId tenantId = TenantContextHolder.get();
        if (tenantId == null) {
            return;
        }
        entityManager
            .createNativeQuery("SELECT set_config('app.current_tenant', :tenantId, true)")
            .setParameter("tenantId", tenantId.value().toString())
            .getSingleResult();
    }

    private static Tenant toDomain(TenantJpaEntity entity) {
        return new Tenant(new TenantId(entity.getId()), entity.getName());
    }
}
