package com.caas.notification.infrastructure.websocket;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

// Milestone 18 (ADR-0030): o ID token vem no header Authorization do CONNECT (o navegador não
// consegue mandá-lo no handshake do WebSocket); o tenant do claim vira o principal da sessão e o
// SUBSCRIBE só é aceito para o tópico desse tenant. Sem tenant válido, a conexão é recusada.
@Component
public class TenantAuthChannelInterceptor implements ChannelInterceptor {

    private static final String TENANT_CLAIM = "custom:tenant_id";
    private static final String TOPIC_PREFIX = "/topic/tenants/";

    private final JwtDecoder jwtDecoder;

    public TenantAuthChannelInterceptor(@Value("${app.cognito.jwk-set-uri}") String jwkSetUri) {
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorize(accessor);
        }
        return message;
    }

    private java.security.Principal authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MessagingException("token ausente");
        }
        String tenant;
        try {
            tenant = jwtDecoder.decode(authorization.substring("Bearer ".length())).getClaimAsString(TENANT_CLAIM);
            UUID.fromString(tenant);
        } catch (RuntimeException e) {
            throw new MessagingException("token inválido ou sem tenant");
        }
        return () -> tenant;
    }

    private void authorize(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        String tenant = accessor.getUser() == null ? null : accessor.getUser().getName();
        if (tenant == null || destination == null || !destination.startsWith(TOPIC_PREFIX + tenant + "/")) {
            throw new MessagingException("assinatura fora do tenant da sessão");
        }
    }
}
