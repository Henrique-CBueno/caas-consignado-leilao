# 0032 — Contagem regressiva do leilão ao vivo

## Status
Aceita

## Contexto
O dashboard não sabia quando o leilão ia fechar nem se já tinha aberto: a tela ficava vazia até o primeiro lance. O evento de abertura (`AuctionOpened`) já traz `expiresAt`, mas o serviço de notificações só repassava lances e fechamento.

## Decisão
- **`AUCTION_OPENED` no mesmo tópico por tenant** ([ADR-0030](0030-websocket-autenticado-por-tenant-e-cors-restrito.md)): o `notification-gateway-service` consome `auction.opened` e publica em `/topic/tenants/{tenantId}/auctions/{proposalId}`; só o tenant dono recebe. Nenhum contrato de evento mudou.
- **Front**: o feed expõe `opened` (o prazo). Enquanto há prazo e o leilão não fechou, Ao vivo mostra "Leilão aberto" e "Fecha em mm:ss · às hh:mm:ss"; ao chegar o fechamento a contagem some; passado o prazo sem fechamento mostra "Aguardando o fechamento" (nunca tempo negativo). Sem abertura recebida, nada é mostrado: a tela não estima um prazo que não recebeu. O modo `?demo` simula a abertura.
- **Relógio injetável** (`CLOCK`, mesmo padrão do feed, da navegação e da autenticação): adaptador real atualizado a cada segundo e um falso nos testes.
- **Acessibilidade**: o leitor de tela anuncia o estado (aberto, aguardando), não o tique; a contagem é texto, sem animação.

## Consequências
- A contagem compara o relógio do navegador com o `expiresAt` do servidor; a diferença entre os relógios é aceita na demonstração.
- A abertura não é reenviada a quem começa a acompanhar depois dela (ex.: pela lista de Propostas): nesses casos não há contagem. Corrigir exigiria consultar o estado do leilão na API (fora de escopo).
- O ADR-0030 passa a listar três tipos de notificação: `AUCTION_OPENED`, `BID_PLACED` e `AUCTION_CLOSED`.
