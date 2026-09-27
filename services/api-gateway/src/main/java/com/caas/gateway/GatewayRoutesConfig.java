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
        @Value("${app.routes.proposal-service-uri}") String proposalServiceUri
    ) {
        return builder.routes()
            .route("tenant-service", r -> r.path("/tenants/**").uri(tenantServiceUri))
            .route("proposal-service", r -> r.path("/proposals/**").uri(proposalServiceUri))
            .build();
    }
}
