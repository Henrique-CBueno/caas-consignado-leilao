package com.caas.gateway

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayRoutesConfig {
    @Bean
    fun routes(
        builder: RouteLocatorBuilder,
        @Value("\${app.routes.tenant-service-uri}") tenantServiceUri: String,
    ): RouteLocator =
        builder.routes()
            .route("tenant-service") { r -> r.path("/tenants/**").uri(tenantServiceUri) }
            .build()
}
