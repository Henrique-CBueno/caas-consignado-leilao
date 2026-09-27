package com.caas.tenant.domain

import java.util.UUID

@JvmInline
value class TenantId(val value: UUID)

data class Tenant(
    val id: TenantId,
    val name: String,
)
