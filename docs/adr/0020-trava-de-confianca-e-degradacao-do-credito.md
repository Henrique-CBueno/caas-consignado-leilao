# 0020 — Trava de confidence < 60% e degradação graciosa da decisão de crédito

## Status
Aceita

## Contexto
A decisão de crédito vem de um modelo (Jev via OpenRouter) cuja confiança varia e cujo serviço pode falhar. Uma aprovação de baixa confiança, ou um erro engolido, é pior que pedir revisão humana.

## Decisão
O `CreditDecisionService`, na camada `application`, aplica a trava independentemente do adaptador: `confidence < 0.6` → `MANUAL_REVIEW`. Assim ela vale para o `JevOpenRouterAdapter` e para o `MockDecisionAdapter` sem duplicação (ADR-0005). Além disso, **qualquer falha** do `CreditDecisionPort` (Jev indisponível, circuito aberto, limite de taxa) degrada para `MANUAL_REVIEW` com `confidence 0.0`. A decisão roda em duas etapas: `PRE_AUCTION` (abre o leilão) e `POST_AUCTION` (revalida o vencedor antes do contrato).

## Consequências
- Antes da Milestone 11 a exceção subia crua até o `@KafkaListener`; a degradação graciosa prometida nunca funcionou de fato — foi um bug real achado e corrigido, com teste.
- `MANUAL_REVIEW` é estado terminal: não há fila de análise humana implementada. O limiar 0,6 está em código, não em configuração.
