# 0026 — Metodologia spec-first e TDD com GitHub Issues como tracker

## Status
Aceita

## Contexto
Desenvolver nove serviços sozinho pede disciplina para não construir por impulso nem testar detalhes de implementação.

## Decisão
Cada milestone segue: (1) exploração e proposta de **seams** de teste, confirmados com o autor; (2) uma spec publicada como GitHub issue com o label `ready-for-agent` (skill `to-spec`, executada pelo autor); (3) implementação com a skill `tdd`: um teste vermelho por vez em fatias verticais, apenas nos seams confirmados, nenhum código de produção antes de um teste falhando; (4) validação real (cluster, `make smoke-test`); (5) commit por milestone e fechamento da issue. Mutações (quebrar o código de propósito e ver o teste falhar) foram usadas como prova de que os testes de contrato e de propagação detectam o que dizem detectar. As issues #1 a #16 registram cada spec.

## Consequências
- Custo de tempo por milestone; em troca, os seams ficaram estáveis e os achados reais surgiram dos testes (status HTTP ignorado no bot, degradação do crédito nunca funcional, trace cortado pelo outbox).
- O tracker é um repositório privado; o histórico de decisões públicas está nos ADRs.
