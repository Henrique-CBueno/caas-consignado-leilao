# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

**Usuário principal (para decisões de design): o apresentador ou avaliador da demonstração.** Quem mostra o projeto em entrevista técnica, ou quem o avalia como portfólio: costuma estar com a tela projetada ou compartilhada, tem poucos minutos e precisa entender a disputa em segundos, sem explicação prévia. A ficção do produto é o **analista de um banco (tenant)** que acompanha o leilão de uma proposta de crédito consignado; ela é o cenário da demonstração, não o usuário que orienta a densidade da tela.

Tarefa do usuário: informar (ou reabrir) o ID de uma proposta e ver o leilão reverso acontecer ao vivo: quem está ganhando, como a disputa muda e quem vence.

## Product Purpose

Plataforma **simulada** de Credit-as-a-Service para leilão reverso de crédito consignado, construída como projeto de portfólio técnico (não é um produto real). Uma proposta atravessa nove serviços por eventos: análise de crédito, leilão entre financiadores (bots), revalidação, contrato e desembolso. O front é a janela ao vivo desse fluxo. Sucesso: quem vê a tela entende a disputa e a regra de desempate sem ler documentação, e enxerga que o sistema por trás é real e rigoroso.

## Positioning

Três coisas que uma interface genérica de "lista de lances" não poderia afirmar com verdade:

- **Leilão reverso ao vivo com regra explícita:** financiadores disputam a menor taxa em tempo real, e o desempate (menor taxa, depois menor prazo, depois lance mais antigo) é uma regra conhecida e visível, não uma caixa-preta.
- **Fluxo ponta a ponta observável:** uma proposta gera um trace único atravessando os serviços, métricas e isolamento entre tenants demonstráveis.
- **Engenharia como produto:** a qualidade técnica (testes, ADRs, Kubernetes) é o argumento do projeto, e a interface deve refletir esse rigor.

## Operating Context

- Demonstração ao vivo, muitas vezes com a tela projetada ou compartilhada, no navegador de um notebook; o roteiro completo está em `docs/demo.md`.
- Cenário típico: o apresentador cria uma proposta pelo gateway, cola o ID na tela e acompanha a disputa; leilões duram cerca de 45 segundos e os lances chegam em poucos segundos.
- Também roda contra o cluster (NodePort do minikube) e, sem cluster, em modo demonstração (`?demo`) com lances simulados.
- Vocabulário do domínio em `CONTEXT.md` (proposta, leilão reverso, lance, financiador, vencedor, desembolso, tenant).

## Capabilities and Constraints

- O front recebe apenas dois tipos de evento por WebSocket (STOMP): **lance registrado** (financiador, taxa, prazo em meses, horário) e **leilão fechado** (status, vencedor, taxa vencedora). **Não há prazo (`expiresAt`) nem evento de abertura**, portanto não existe contagem regressiva; não existe API para listar leilões ou propostas.
- O WebSocket é público, sem autenticação; o front não faz login. O histórico é **local ao navegador** (armazenamento do navegador), sem backend.
- A ordem do ranking espelha a regra do backend (ADR-0018): menor taxa, depois menor prazo, depois lance mais antigo.
- Stack existente (Angular 22, roteamento por hash, sem dependências novas de runtime); orçamento de build de 500 kB de aviso e 1 MB de erro no bundle inicial.
- Idioma da interface: **pt-BR**. Tema claro e escuro. A demo pode rodar offline: nada de fontes ou ativos carregados de terceiros na execução.
- Indecidido: se algum dia o backend expuser prazo do leilão ou listagem, a tela passa a ter contagem regressiva e navegação de leilões; até lá, nada disso deve ser sugerido pela interface.

## Brand Commitments

- Nome do produto: **CaaS Consignado Leilão**. Textos em pt-BR, alinhados ao glossário do projeto.
- Não há logotipo, paleta ou identidade visual pré-existentes: livre para criar o mundo visual (a direção é decidida na etapa de design, não aqui).
- O modo demonstração deve ser sempre sinalizado com clareza, para nunca ser confundido com dados reais.

## Evidence on Hand

- Capturas reais do sistema em execução: `docs/img/dashboard-leilao.png` (tela atual), `docs/img/jaeger-trace.png` (trace de 7 serviços) e `docs/img/grafana-dashboard.png`.
- Roteiro de demonstração executado de ponta a ponta: `docs/demo.md`; decisões em `docs/adr/`; diagramas em `docs/c4/`.
- Formato real dos dados de um leilão (exemplo observado): três lances de financiadores diferentes (`funder-1`, `funder-2`, `funder-3`), taxas entre cerca de 1,9% e 2,6%, prazos de 12, 18 e 24 meses, fechamento com vencedor.
- **Ausências que o design não deve inventar:** clientes, depoimentos, métricas de uso, financiadores reais, logotipo, preços. Os financiadores são bots simulados e os dados são de demonstração.

## Product Principles

1. **A disputa se lê em segundos.** Quem lidera, por quanto e por quê deve saltar aos olhos antes de qualquer detalhe.
2. **A regra é parte da interface.** O critério de desempate aparece onde ajuda a entender o resultado; nada de vencedor "mágico".
3. **Ao vivo é verdade, e simulado é dito.** O estado da conexão é sempre honesto; a demonstração é sinalizada; nada de contagem regressiva ou dado que o sistema não fornece.
4. **Rigor visível nos detalhes.** A interface de uma ferramenta de engenharia precisa parecer precisa: alinhamento, números, estados e transições cuidados, sem enfeite que não comunique estado.
5. **Serve à demonstração.** Legível projetada, previsível na história (começo, virada, fim) e sem depender de explicação de quem apresenta.

## Accessibility & Inclusion

Padrão exigido: **WCAG AA** nos dois temas (contraste, foco visível, uso completo por teclado, região viva para lances e resultado, respeito a `prefers-reduced-motion`). Deve funcionar em telas pequenas (a partir de 320 px) além de projetada em telas grandes.
