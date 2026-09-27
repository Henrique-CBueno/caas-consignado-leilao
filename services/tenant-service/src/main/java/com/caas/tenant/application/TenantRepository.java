package com.caas.tenant.application;

import com.caas.tenant.domain.Tenant;
import com.caas.tenant.domain.TenantId;

public interface TenantRepository {
    Tenant save(Tenant tenant);

    Tenant findById(TenantId id);
}
