# 0006 — `notification-gateway-service`: stack Servlet + STOMP em vez de WebFlux

## Status
Aceita

## Contexto
O plano original descreve o `notification-gateway-service` como "Spring WebFlux + STOMP". Suporte STOMP com broker (`@EnableWebSocketMessageBroker`, `SimpMessagingTemplate`, broker simples in-memory) é uma feature do stack Servlet do Spring (`spring-boot-starter-websocket`) — não existe hoje um equivalente reativo de primeira classe igualmente maduro no WebFlux para esse mesmo modelo de broker/assinatura por destino.

## Decisão
`notification-gateway-service` usa `spring-boot-starter-websocket` (stack Servlet), não WebFlux. O serviço não guarda estado de domínio e não faz I/O bloqueante custoso (é puro rebroadcast de evento Kafka já consumido de forma assíncrona pelo próprio `spring-kafka`) — o principal argumento para WebFlux (não bloquear threads sob I/O pesado) não se aplica aqui. Usar o stack reativo só para essa peça introduziria complexidade (bridging manual entre `WebSocketHandler` reativo e o broker STOMP) sem ganho real.

## Consequências
- Endpoint STOMP único em `/ws`, sem SockJS (o dashboard roda em navegador moderno, não precisa do fallback de long-polling).
- Broker STOMP simples (in-memory, via `SimpMessagingTemplate`) — suficiente com uma única réplica do serviço. Múltiplas réplicas exigiriam um relay externo (ex.: STOMP sobre RabbitMQ/Redis) para o fan-out funcionar entre instâncias; não implementado agora, mesmo critério de migração futura já usado para outras simplificações do projeto (ex.: RLS single-schema).
- Mesmo padrão de correção de plano já usado nas ADR-0002 (limitações do LocalStack Community) e ADR-0004 (Kotlin → Java): o plano original é ajustado à luz de uma restrição real encontrada durante a implementação, sem reabrir a decisão de arquitetura por trás dela (rebroadcast de evento de domínio via WebSocket, isolado por leilão).
