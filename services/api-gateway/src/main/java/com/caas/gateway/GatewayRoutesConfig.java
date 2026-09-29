package com.caas.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator routes(
        RouteLocatorBuilder builder,
        @Value("${app.routes.tenant-service-uri}") String tenantServiceUri,
        @Value("${app.routes.proposal-service-uri}") String proposalServiceUri,
        @Value("${app.routes.disbursement-service-uri}") String disbursementServiceUri
    ) {
        return builder.routes()
            .route("tenant-service", r -> r.path("/tenants/**", "/admin/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("tenant-service-cb").setFallbackUri("forward:/fallback")))
                .uri(tenantServiceUri))
            .route("proposal-service", r -> r.path("/proposals/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("proposal-service-cb").setFallbackUri("forward:/fallback")))
                .uri(proposalServiceUri))
            .route("disbursement-service", r -> r.path("/disbursements/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("disbursement-service-cb").setFallbackUri("forward:/fallback")))
                .uri(disbursementServiceUri))
            .build();
    }
}
