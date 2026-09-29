package com.caas.tenant.infrastructure.persistence;

import com.caas.tenant.application.AdminTenantRepository;
import com.caas.tenant.domain.Tenant;
import com.caas.tenant.domain.TenantId;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

// Caminho separado do JpaTenantRepository: assume admin_role (BYPASSRLS, só SELECT/INSERT)
// em vez de app_role + app.current_tenant, para o isolamento por tenant não ser tocado.
@Repository
public class JpaAdminTenantRepository implements AdminTenantRepository {

    private final SpringDataTenantRepository springDataRepository;
    private final EntityManager entityManager;

    public JpaAdminTenantRepository(SpringDataTenantRepository springDataRepository, EntityManager entityManager) {
        this.springDataRepository = springDataRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tenant> list() {
        assumeAdminRole();
        return springDataRepository.findAll().stream().map(JpaAdminTenantRepository::toDomain).toList();
    }

    @Override
    @Transactional
    public Tenant create(String name) {
        assumeAdminRole();
        var entity = new TenantJpaEntity(UUID.randomUUID(), name);
        entityManager.persist(entity);
        entityManager.flush();
        return toDomain(entity);
    }

    private void assumeAdminRole() {
        entityManager.createNativeQuery("SET LOCAL ROLE admin_role").executeUpdate();
    }

    private static Tenant toDomain(TenantJpaEntity entity) {
        return new Tenant(new TenantId(entity.getId()), entity.getName());
    }
}
