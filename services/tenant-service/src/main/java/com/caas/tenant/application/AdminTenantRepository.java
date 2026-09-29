package com.caas.tenant.application;

import com.caas.tenant.domain.Tenant;
import java.util.List;

public interface AdminTenantRepository {
    List<Tenant> list();

    Tenant create(String name);
}
