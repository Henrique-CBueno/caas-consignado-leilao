# 0019 — Financiadores simulados como serviço separado e injeção manual para demo

## Status
Aceita

## Contexto
Sem financiadores reais, o leilão precisa de participantes; misturá-los ao `auction-service` esconderia a fronteira REST real entre quem organiza o leilão e quem dá lances.

## Decisão
O `funder-bot-service` mantém uma população fixa de bots (`funder-1..3`, com taxa-base e prazo em `app.funder-bot.bots`). Ao receber `AuctionOpened`, cada bot elegível agenda um lance após atraso aleatório (500–3000 ms) com taxa-base ± jitter de até 0,1 ponto, e o envia por HTTP a `POST /auctions/{id}/bids` protegido por Circuit Breaker, sem retry (o bot simplesmente não participa se falhar). A **injeção manual para demo** é o mesmo endpoint: um `curl` com `funderId` de um elegível.

## Consequências
- A fronteira bot → leilão é a única REST interna real e tem contrato Pact (ADR-0010).
- O bot não modela risco nem estratégia; substituí-lo por financiadores reais exigiria um adaptador (webhook ou API) no mesmo endpoint.
