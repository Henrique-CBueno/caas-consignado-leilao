package com.caas.tenant.infrastructure.web;

import com.caas.tenant.application.TenantRepository;
import com.caas.tenant.domain.Tenant;
import com.caas.tenant.domain.TenantId;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantController {

    private final TenantRepository tenantRepository;

    public TenantController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/tenants/{id}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(new TenantId(id));
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new TenantResponse(tenant.id().value(), tenant.name()));
    }
}
