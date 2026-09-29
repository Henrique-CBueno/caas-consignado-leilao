package com.caas.tenant.infrastructure.web;

import com.caas.tenant.application.AdminTenantRepository;
import com.caas.tenant.application.DemoUserProvisioner;
import com.caas.tenant.domain.Tenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Milestone 17 (ADR-0029). O gateway só encaminha /admin/** para identidades com custom:role=admin.
@RestController
public class AdminTenantController {

    public record CreateTenantRequest(String name) {
    }

    private final AdminTenantRepository repository;
    private final DemoUserProvisioner provisioner;

    public AdminTenantController(AdminTenantRepository repository, DemoUserProvisioner provisioner) {
        this.repository = repository;
        this.provisioner = provisioner;
    }

    @Operation(summary = "Lista todos os tenants (administrador)")
    @GetMapping("/admin/tenants")
    public List<TenantResponse> list() {
        return repository.list().stream().map(t -> new TenantResponse(t.id().value(), t.name())).toList();
    }

    @Operation(summary = "Cria um tenant e o usuário de demonstração dele (administrador)", responses = {
        @ApiResponse(responseCode = "201", description = "Tenant criado"),
        @ApiResponse(responseCode = "400", description = "Nome vazio", content = @io.swagger.v3.oas.annotations.media.Content),
        @ApiResponse(responseCode = "200", description = "Tenant já existia sem usuário demo; o usuário foi provisionado agora"),
        @ApiResponse(responseCode = "409", description = "Nome já existente", content = @io.swagger.v3.oas.annotations.media.Content),
        @ApiResponse(responseCode = "502", description = "Tenant criado mas o usuário demo não pôde ser provisionado (repetir o pedido completa)", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping("/admin/tenants")
    public ResponseEntity<?> create(@RequestBody CreateTenantRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || com.caas.tenant.infrastructure.CognitoDemoUserProvisioner.usernameFor(name).startsWith("@")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Informe o nome do tenant."));
        }
        // Idempotente: um tenant gravado sem usuário demo (falha do provedor na criação anterior) é
        // completado ao repetir o pedido (200); se o usuário já existia, é duplicata (409).
        Tenant tenant;
        boolean existed = false;
        try {
            tenant = repository.create(name);
        } catch (DataIntegrityViolationException e) {
            tenant = repository.list().stream().filter(t -> t.name().equals(name)).findFirst().orElse(null);
            if (tenant == null) {
                return conflict();
            }
            existed = true;
        }
        boolean provisioned;
        try {
            provisioned = provisioner.provision(tenant);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "message", "Tenant criado, mas o usuário de demonstração não pôde ser provisionado. Repita para completar."));
        }
        if (!provisioned) {
            return conflict();
        }
        return ResponseEntity.status(existed ? HttpStatus.OK : HttpStatus.CREATED)
            .body(new TenantResponse(tenant.id().value(), tenant.name()));
    }

    private static ResponseEntity<?> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Já existe um tenant com esse nome."));
    }
}
