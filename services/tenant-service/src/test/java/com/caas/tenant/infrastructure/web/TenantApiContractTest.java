package com.caas.tenant.infrastructure.web;

import com.caas.openapitesting.OpenApiSpec;
import com.caas.openapitesting.WithSpringDoc;
import com.caas.tenant.application.AdminTenantRepository;
import com.caas.tenant.application.DemoUserProvisioner;
import com.caas.tenant.application.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({TenantController.class, AdminTenantController.class})
@WithSpringDoc
class TenantApiContractTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TenantRepository tenantRepository;

    @MockitoBean
    AdminTenantRepository adminTenantRepository;

    @MockitoBean
    DemoUserProvisioner demoUserProvisioner;

    @Test
    void publishedOpenApiMatchesTheRealApi() throws Exception {
        OpenApiSpec.assertMatchesCommitted(mockMvc, "tenant-service");
    }
}
