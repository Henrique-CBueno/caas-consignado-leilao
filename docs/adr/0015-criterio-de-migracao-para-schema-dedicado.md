# 0015 — Critério de migração para schema ou banco dedicado por tenant

## Status
Aceita

## Contexto
RLS com esquema compartilhado é o ponto de partida mais barato, mas tem teto: um tenant grande ou regulado pode exigir mais isolamento.

## Decisão
Permanecer em esquema compartilhado com RLS até que um destes critérios seja verdadeiro: (1) exigência regulatória de isolamento físico ou chave de criptografia dedicada; (2) *noisy neighbor* medido (latência p95 ou contenção de locks atribuível a um tenant); (3) necessidade de backup/restore ou retenção por tenant; (4) SLA diferenciado. O caminho seria um schema por tenant com roteamento de `DataSource` pelo `TenantContext`, mantendo a RLS como defesa em profundidade. **Nada disto está implementado**; é um critério de decisão, não trabalho pendente.

## Consequências
- Evita complexidade operacional (N schemas, N migrations) antes de existir necessidade medida.
- A migração futura mexe na camada de persistência de cada serviço, não nos contratos de API nem nos eventos.
