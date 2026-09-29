package com.caas.tenant.application;

import com.caas.tenant.domain.Tenant;

// Cria o usuário de demonstração do tenant no provedor de identidade (Cognito emulado).
public interface DemoUserProvisioner {
    void provision(Tenant tenant);
}
