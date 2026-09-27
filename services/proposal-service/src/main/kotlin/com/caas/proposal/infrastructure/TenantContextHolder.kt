package com.caas.proposal.infrastructure

import com.caas.proposal.domain.TenantId

object TenantContextHolder {
    private val current = ThreadLocal<TenantId>()

    fun set(tenantId: TenantId) = current.set(tenantId)

    fun get(): TenantId? = current.get()

    fun clear() = current.remove()
}
