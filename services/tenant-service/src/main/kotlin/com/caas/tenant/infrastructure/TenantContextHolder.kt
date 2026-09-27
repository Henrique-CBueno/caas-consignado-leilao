package com.caas.tenant.infrastructure

import com.caas.tenant.domain.TenantId

object TenantContextHolder {
    private val current = ThreadLocal<TenantId>()

    fun set(tenantId: TenantId) = current.set(tenantId)

    fun get(): TenantId? = current.get()

    fun clear() = current.remove()
}
