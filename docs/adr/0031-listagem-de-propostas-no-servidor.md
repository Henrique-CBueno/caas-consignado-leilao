# 0031 — Listagem de propostas no servidor

## Status
Aceita

## Contexto
O front só conhecia as propostas do histórico local do navegador: trocar de navegador ou limpar os dados perdia a lista, e não havia como um tenant ver o que já originou.

## Decisão
- **`GET /proposals`** no `proposal-service` devolve uma página das propostas do tenant, da mais recente para a mais antiga (`page` a partir de 0, `size` de 1 a 100, padrão 20; a resposta traz `hasNext`, sem contagem total). Cada item tem id, tomador, valor, prazo e data de criação.
- **Isolamento sem caminho novo**: a consulta usa o mesmo mecanismo da consulta por id (`SET LOCAL ROLE app_role` + `app.current_tenant`); sem contexto de tenant a RLS devolve vazio. Nenhuma role nova e nenhuma mudança no gateway (a rota, o claim de tenant e o CORS já cobriam `/proposals`; a identidade admin continua com 403).
- **Ordem determinística**: data de criação decrescente e, no desempate, id, para a paginação não repetir nem pular linhas. A migração V6 adicionou `created_at` (`DEFAULT now()`); propostas anteriores recebem a data da migração.
- **Sem status na lista**: o status só existe como `PENDING_CREDIT_ANALYSIS` e nada o atualiza (o serviço não consome os eventos do leilão nem do desembolso). Mostrá-lo enganaria; o desfecho segue no acompanhamento ao vivo, para onde cada linha leva.
- **Front**: tela **Propostas** (tiras no vocabulário do Rack), com estado vazio, erro inline e "Carregar mais"; o atalho aparece só para a sessão de tenant.

## Consequências
- Um tenant recupera suas propostas em qualquer navegador; o Histórico local continua separado.
- Fazer o status avançar (o `proposal-service` consumir eventos do leilão e do desembolso) fica como trabalho futuro.
- Paginação por deslocamento: adequada ao volume da demonstração; com muito volume, migrar para paginação por cursor.
