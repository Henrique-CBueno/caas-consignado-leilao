package com.caas.tenant.application

import com.caas.tenant.domain.Tenant
import com.caas.tenant.domain.TenantId

interface TenantRepository {
    fun save(tenant: Tenant): Tenant

    fun findById(id: TenantId): Tenant?
}
