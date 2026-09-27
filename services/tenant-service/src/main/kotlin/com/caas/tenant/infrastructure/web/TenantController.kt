package com.caas.tenant.infrastructure.web

import com.caas.tenant.application.TenantRepository
import com.caas.tenant.domain.TenantId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class TenantResponse(val id: UUID, val name: String)

@RestController
class TenantController(private val tenantRepository: TenantRepository) {

    @GetMapping("/tenants/{id}")
    fun getTenant(@PathVariable id: UUID): ResponseEntity<TenantResponse> {
        val tenant = tenantRepository.findById(TenantId(id)) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(TenantResponse(tenant.id.value, tenant.name))
    }
}
