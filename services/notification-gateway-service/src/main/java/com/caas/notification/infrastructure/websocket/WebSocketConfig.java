package com.caas.notification.infrastructure.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// Broker STOMP simples in-memory: suficiente com uma única réplica na demo.
// Múltiplas réplicas exigiriam um relay externo (RabbitMQ/Redis) para o fan-out
// funcionar entre instâncias — critério de migração futura, não implementado agora.
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final TenantAuthChannelInterceptor tenantAuth;
    private final String[] allowedOrigins;

    public WebSocketConfig(
        TenantAuthChannelInterceptor tenantAuth,
        @Value("${app.cors.allowed-origins}") String[] allowedOrigins
    ) {
        this.tenantAuth = tenantAuth;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(tenantAuth);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
    }
}
