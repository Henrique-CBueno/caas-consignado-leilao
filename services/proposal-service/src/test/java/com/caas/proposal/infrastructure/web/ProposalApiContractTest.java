package com.caas.proposal.infrastructure.web;

import com.caas.openapitesting.OpenApiSpec;
import com.caas.openapitesting.WithSpringDoc;
import com.caas.proposal.application.ProposalRepository;
import com.caas.proposal.application.CreateProposalUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProposalController.class)
@WithSpringDoc
class ProposalApiContractTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProposalRepository proposalRepository;

    @MockitoBean
    CreateProposalUseCase createProposalUseCase;


    @Test
    void publishedOpenApiMatchesTheRealApi() throws Exception {
        OpenApiSpec.assertMatchesCommitted(mockMvc, "proposal-service");
    }
}
