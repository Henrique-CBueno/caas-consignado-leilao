package com.caas.auction.infrastructure.web;

import com.caas.openapitesting.OpenApiSpec;
import com.caas.openapitesting.WithSpringDoc;
import com.caas.auction.application.AuctionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuctionController.class)
@WithSpringDoc
class AuctionApiContractTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuctionRepository auctionRepository;


    @Test
    void publishedOpenApiMatchesTheRealApi() throws Exception {
        OpenApiSpec.assertMatchesCommitted(mockMvc, "auction-service");
    }
}
