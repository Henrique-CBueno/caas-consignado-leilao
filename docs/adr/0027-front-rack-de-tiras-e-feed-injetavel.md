# 0027 — Front: origem de dados injetável, modo demonstração e o Rack de tiras

## Status
Aceita

## Contexto
O dashboard Angular era um protótipo de uma tela, sem testes, sem estados de erro e só visível com o cluster inteiro no ar (~5,9 GiB). Para ser a parte visível do portfólio precisava de identidade visual, comportamento verificado por teste e uma forma de ser mostrado e desenvolvido sem cluster.

## Decisão
- **Origem de dados injetável (`AuctionFeed`)**: o WebSocket saiu do componente e virou uma abstração com signals (conexão, lances, fechamento, leilão observado). Dois adaptadores: o STOMP real (a regra de URL anterior: porta de desenvolvimento em `localhost`, NodePort 30086 no cluster) e um feed de demonstração com roteiro fixo, escolhido por `?demo`. Os testes injetam um feed falso. O adaptador STOMP é fino e **não** tem teste automatizado próprio; é validado no cluster real.
- **Testes de comportamento pelo DOM** (Vitest + jsdom, `ng test`): o seam é o componente raiz com o feed falso. Cobrem validação do ID, estados da conexão, ranking do ADR-0018 (incluindo horários com precisões diferentes), líder, ultrapassada, vencedora e não vencedora, resultado, histórico local, tema, atalhos e modo demonstração. O visual não tem teste automatizado: usa o detector da skill `impeccable`, o `ng build` (orçamentos) e capturas.
- **Regra de ranking duplicada no front**: espelha o `WinnerSelector` (ADR-0018), porque o evento de lance não traz a posição. É uma duplicação assumida e coberta pelos casos de empate.
- **Shell e vistas**: cabeçalho com marca, navegação, estado da conexão, links de Trace e Métricas (portas 30686 e 30300 do mesmo host, ocultos em demonstração) e tema; vistas Ao vivo e Histórico. Roteamento por **hash**, que dispensa `try_files` no nginx da imagem. Histórico e tema ficam no armazenamento do navegador, tolerando armazenamento bloqueado; execuções em demonstração aparecem marcadas como simuladas.
- **Mundo visual: Rack de tiras de controle** (direção escolhida numa rolagem de direções, registrada em `PRODUCT.md`, `DESIGN.md` e no brief da superfície): cada lance é uma tira impressa numa posição do rack, com a regra de desempate à vista, carimbo na vencedora, cruz de caneta na ultrapassada e estado sempre por padrão de borda e texto além de cor. Barlow Condensed auto-hospedada (`@fontsource`, OFL) e monoespaçada de sistema para dados; a demonstração roda offline.
- **CI**: job `front` (`npm ci`, `ng test`, `ng build`).

## Consequências
- O front passou a ter 38 testes e verificação no CI; o bundle inicial ficou em ~330 kB (limite de 500 kB).
- Não há contagem regressiva nem lista de leilões: o `notification-gateway-service` não repassa `AuctionOpened`/prazo e não existe API de listagem. O WebSocket segue público e sem autenticação (ADR-0017).
- `npm install` do Vitest falhou por um bug interno do npm (`edgesOut`); a dependência foi instalada com `--legacy-peer-deps`, e o `npm ci` limpo funciona (é o que o CI usa).
- Em produção o Trace e as Métricas seriam links para ferramentas com autenticação, e o feed real poderia trazer a posição de cada lance para eliminar a duplicação do ranking.
