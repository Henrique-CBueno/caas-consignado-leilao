package com.caas.disbursement.infrastructure.web;

import com.caas.openapitesting.OpenApiSpec;
import com.caas.openapitesting.WithSpringDoc;
import com.caas.disbursement.application.DisbursementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DisbursementController.class)
@WithSpringDoc
class DisbursementApiContractTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DisbursementRepository disbursementRepository;


    @Test
    void publishedOpenApiMatchesTheRealApi() throws Exception {
        OpenApiSpec.assertMatchesCommitted(mockMvc, "disbursement-service");
    }
}
