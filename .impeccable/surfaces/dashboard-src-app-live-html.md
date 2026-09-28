---
version: 1
slug: "dashboard-src-app-live-html"
primary_target: "dashboard/src/app/live.html"
related_targets: ["dashboard/src/app/app.html","dashboard/src/app/history.ts"]
---

# Brief da superfície: dashboard (Ao vivo e Histórico)

## Escopo e modo
Modo **Operate**. Shell (cabeçalho, navegação, estado da conexão, tema, links de observabilidade) e duas vistas: **Ao vivo** (acompanhar um leilão reverso) e **Histórico** (leilões vistos neste navegador). Substituição do visual: o anterior é evidência do que a tela faz, não autoridade.

## Público, tarefa e prova
Apresentador ou avaliador da demo, com a tela projetada. Tarefa: informar ou reabrir o ID de uma proposta e entender a disputa em segundos. Prova: lances reais (financiador, taxa, prazo, horário) e a regra de desempate visível. Nada de prazo ou contagem regressiva (o sistema não fornece).

## Direção escolhida e momento memorável
Rack de tiras de controle. Momento memorável: uma tira nova entra no rack e a líder muda de posição; no fechamento, a tira vencedora é marcada. Aviação entra só na gramática (tira, suporte, rack); textos em pt-BR simples.

## Decisões em aberto (resolvidas no build e registradas no DESIGN.md ao fim)
Paleta exata, famílias tipográficas (no máximo duas, auto-hospedadas) e técnica de movimento.

## Direction contract

THESIS: A disputa é um rack de tiras: cada lance é uma tira que desliza para a posição que a regra de desempate lhe dá, e a regra fica impressa à vista. Recusa a lista de linhas iguais, os cartões soltos sobre neutro e o placar de trading em verde e vermelho.

OWN-WORLD: Baia de controle de tráfego aéreo. Rack grafite-esverdeado como campo único de cor, nos temas claro (luz de sala) e escuro (cabine à noite). Tiras em papel marfim com rótulos e numerais em condensada de família única; dados (ID, horário) em monoespaçada. Suportes de plástico por estado: azul para líder, âmbar para em disputa, cinza para ultrapassada. Caneta vermelha para marcas (cruz de ultrapassada, aba dobrada de "onde parei"). Estado com padrão de borda (sólida, tracejada, dupla) e rótulo de texto, nunca só cor. Sem listras decorativas, sem cartões aninhados, sem borda lateral colorida grossa, sem sombra dura, elevação declarada uma vez.

STORY: Quem vê entende em segundos quem lidera, por quanto e por quê (a regra está no cabeçalho), acompanha a virada e o fechamento, e acredita porque cada tira é dado real do fluxo. Age colando um ID, reabrindo um leilão do histórico ou ligando o modo demonstração.

FIRST VIEWPORT: Desktop 1440×900. Barra fina no topo do rack com marca, navegação Ao vivo e Histórico, estado da conexão em texto, links Trace e Métricas e alternador de tema. Abaixo, à esquerda (cerca de 280 px), a regra de desempate em três linhas de prioridade. Ao centro, o campo de ID com a ação Acompanhar e, logo abaixo, o rack: tiras de 72 px em ordem de posição, com a líder em duas linhas de altura e a taxa em numerais grandes (cerca de 64 px). Vazio: rack sem tiras, atalhos dos últimos leilões e a ação "Ver com dados simulados". No celular, tiras em duas linhas e a regra recolhível.

FORM: Rack de tiras de controle de tráfego aéreo; direção atribuída pela rolagem (candidato 3 da lista ordenada), chave de seed 5240fd59.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
