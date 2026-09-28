# 0018 — Mecanismo do leilão reverso e regra de desempate

## Status
Aceita

## Contexto
Financiadores competem para oferecer a menor taxa por uma proposta aprovada. É preciso definir quando o leilão abre e fecha, quem vence e como resolver empates.

## Decisão
O leilão abre ao consumir `credit.decision.made` com decisão `APPROVE` na etapa `PRE_AUCTION`, com janela de `app.auction.window-seconds` (45 s por padrão) e financiadores elegíveis em `app.auction.eligible-funder-ids`. Lances entram por `POST /auctions/{proposalId}/bids` (`409` se fechado ou expirado, `404` se inexistente). O `AuctionCloser` fecha os expirados a cada `closer-fixed-delay-ms`. O vencedor (`WinnerSelector`) é a **menor taxa**; empate → **menor prazo**; novo empate → **lance mais antigo**. Sem lances, o leilão termina `CLOSED_NO_WINNER`. Os eventos `AuctionOpened`, `AuctionBidPlaced` e `AuctionClosed` saem via outbox; o dashboard acompanha em tempo real por STOMP (ADR-0006).

## Consequências
- Simplificações conscientes: *last-write-wins* em lances simultâneos (sem update condicional) e relógio do servidor como única referência de tempo.
- Em produção: update condicional do DynamoDB (`attribute_exists` + `status = OPEN`) com lances como lista nativa, idempotência de lance e sequência monotônica por leilão.
