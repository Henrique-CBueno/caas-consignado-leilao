# 0030 — WebSocket autenticado por tenant, CORS restrito e reparo do tenant sem usuário

## Status
Aceita

## Contexto
O WebSocket do `notification-gateway-service` era público (ADR-0017): quem soubesse o id de uma proposta de outro tenant via lances e vencedor, apesar de a API REST estar protegida por JWT e RLS. O CORS do gateway era `*` (ADR-0028) e um tenant criado pelo administrador podia ficar sem usuário demo quando o provedor de identidade falhava (ADR-0029).

## Decisão
- **JWT no `CONNECT` do STOMP.** O navegador não envia `Authorization` no handshake do WebSocket, então o ID token viaja no header do frame `CONNECT`. Um interceptor valida o token contra o mesmo JWKS do gateway e usa o claim `custom:tenant_id` como principal da sessão. Token ausente, inválido ou sem tenant (inclui a identidade administrativa) recusa a conexão.
- **Tópicos por tenant**: `/topic/tenants/{tenantId}/auctions/{proposalId}`. O `SUBSCRIBE` só é aceito se o `{tenantId}` do tópico for o da sessão, e o listener do Kafka publica só no tópico do `tenantId` do evento (os eventos de lance e de fechamento já o carregavam: nenhum contrato mudou, nenhuma consulta a outro serviço). Um tenant que assine o id de uma proposta alheia no próprio tópico simplesmente não recebe nada.
- **Front**: Ao vivo exige sessão (sem ela, oferece Entrar); o feed STOMP manda o token no `CONNECT` e assina o tópico do tenant do token. O modo `?demo` continua público (feed simulado, sem rede).
- **CORS configurável** (`APP_CORS_ALLOWED_ORIGINS`, lista): o gateway (`/auth`, `/proposals`, `/admin`) e o handshake do WebSocket só aceitam a origem do dashboard; o `make deploy-local` passa `http://<ip do minikube>:30090`, o padrão de desenvolvimento é `http://localhost:4200`. Origem fora da lista é recusada no preflight.
- **Criação de tenant idempotente**: repetir `POST /admin/tenants` para um nome que já existe mas cujo usuário demo não existe cria o usuário e responde 200; se o usuário já existia, 409. O 502 (provedor inacessível) deixa de ser um estado sem saída: repetir o pedido repara.

Tipos de notificação no tópico: `AUCTION_OPENED` (Milestone 20, [ADR-0032](0032-contagem-regressiva-do-leilao.md)), `BID_PLACED` e `AUCTION_CLOSED`.

## Consequências
- O isolamento entre tenants vale agora também no tempo real; testado com Kafka e `cognito-local` reais (conexão sem token, com token inválido e da identidade admin recusadas; assinatura do tópico alheio recusada; evento de outro tenant nunca entregue).
- Limitação assumida: sem TLS neste ambiente (`ws://`, `http://`), o token trafega em claro no frame STOMP, como já trafegava no `Authorization` do gateway. Em produção: `wss://` e `https://`.
- O token só é validado no `CONNECT`: uma conexão aberta sobrevive à expiração do token (sem renovação nesta milestone).
- Continua fora de escopo: claim de tenant só no ID token, senha demo fixa, relay externo para várias réplicas do serviço de notificações.
