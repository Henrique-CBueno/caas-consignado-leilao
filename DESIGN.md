---
name: CaaS Consignado Leilão
description: Rack de tiras de controle de tráfego aéreo para o leilão reverso ao vivo. Papel marfim sobre grafite-esverdeado, suporte colorido por estado, caneta vermelha para marcas.
colors:
  rack: "#1c2622"
  rack-2: "#243029"
  rack-line: "#3a4941"
  control-line: "#6d8177"
  ink: "#e8eee9"
  ink-2: "#aebbb3"
  paper: "#f2eedd"
  paper-ink: "#1a1e1c"
  paper-ink-2: "#4f5651"
  strip-edge: "#0f1512"
  leader: "#8db4e0"
  contend: "#e0ab45"
  passed: "#8a948e"
  pen: "#b3271a"
  pen-on-rack: "#f08a78"
  accent-text: "#e0ab45"
  focus: "#f0c75e"
  action-bg: "#e0ab45"
  action-ink: "#1a1e1c"
  state-ok: "#8fd19e"
  state-wait: "#e0ab45"
  state-off: "#aebbb3"
  rack-light: "#b4c1b8"
  rack-2-light: "#c5d0c8"
  rack-line-light: "#8b9d92"
  control-line-light: "#4a5d52"
  ink-light: "#131d18"
  ink-2-light: "#2b3a32"
  paper-light: "#fbf8ec"
  paper-ink-light: "#171b19"
  paper-ink-2-light: "#4a514c"
  strip-edge-light: "#33403a"
  leader-light: "#2b5c97"
  contend-light: "#b7791a"
  passed-light: "#6d7872"
  pen-on-rack-light: "#8f1d12"
  accent-text-light: "#6b4300"
  focus-light: "#0b3d91"
  action-bg-light: "#16241c"
  action-ink-light: "#f4f0e0"
  state-ok-light: "#1d6b34"
  state-wait-light: "#8a5a00"
  state-off-light: "#2b3a32"
typography:
  display:
    fontFamily: "'Barlow Condensed', 'Arial Narrow', sans-serif"
    fontSize: "4rem"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "normal"
  headline:
    fontFamily: "'Barlow Condensed', 'Arial Narrow', sans-serif"
    fontSize: "2rem"
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: "normal"
  title:
    fontFamily: "'Barlow Condensed', 'Arial Narrow', sans-serif"
    fontSize: "1.5rem"
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: "normal"
  body:
    fontFamily: "'Barlow Condensed', 'Arial Narrow', sans-serif"
    fontSize: "1.125rem"
    fontWeight: 500
    lineHeight: 1.35
    letterSpacing: "normal"
    fontFeature: "font-variant-numeric: tabular-nums"
  label:
    fontFamily: "'Barlow Condensed', 'Arial Narrow', sans-serif"
    fontSize: "1rem"
    fontWeight: 600
    lineHeight: 1.35
    letterSpacing: "0.06em"
  data:
    fontFamily: "ui-monospace, 'SF Mono', Menlo, Consolas, monospace"
    fontSize: "1.0625rem"
    fontWeight: 400
    lineHeight: 1.35
    letterSpacing: "normal"
rounded:
  sm: "2px"
spacing:
  "1": "0.25rem"
  "2": "0.5rem"
  "3": "0.75rem"
  "4": "1rem"
  "6": "1.5rem"
  "8": "2rem"
components:
  strip:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.paper-ink}"
    rounded: "{rounded.sm}"
    height: "4.5rem"
  strip-leader:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.paper-ink}"
    typography: "{typography.display}"
    height: "8rem"
  strip-passed:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.paper-ink-2}"
  holder-leader:
    backgroundColor: "{colors.leader}"
  holder-contending:
    backgroundColor: "{colors.contend}"
  holder-passed:
    backgroundColor: "{colors.passed}"
  rack-frame:
    backgroundColor: "{colors.rack-2}"
    padding: "0.5rem"
  bay-slot:
    textColor: "{colors.ink-2}"
    height: "3rem"
  stamp:
    textColor: "{colors.pen}"
    rounded: "0"
    padding: "0 0.5rem"
  archive-strip:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.paper-ink}"
    rounded: "{rounded.sm}"
    height: "3.5rem"
  bar:
    backgroundColor: "{colors.rack}"
    textColor: "{colors.ink}"
    padding: "0.75rem 1.5rem"
  button:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.sm}"
    height: "2.5rem"
    padding: "0 1rem"
  button-hover:
    backgroundColor: "{colors.rack-2}"
  button-primary:
    backgroundColor: "{colors.action-bg}"
    textColor: "{colors.action-ink}"
    rounded: "{rounded.sm}"
    height: "2.5rem"
    padding: "0 1rem"
  input:
    backgroundColor: "{colors.rack-2}"
    textColor: "{colors.ink}"
    typography: "{typography.data}"
    rounded: "{rounded.sm}"
    height: "2.5rem"
    padding: "0 0.75rem"
---

# Design System: CaaS Consignado Leilão

## Overview

**Creative North Star: "A Baia de Controle"**

A tela é uma baia de controle de tráfego aéreo. Um rack grafite-esverdeado é o único campo de cor; sobre ele, cada lance é uma tira de papel marfim inserida num suporte de plástico. A posição da tira no rack é a posição que a regra de desempate lhe dá, e a regra (menor taxa, depois menor prazo, depois lance mais antigo) fica impressa ao lado. A aviação entra só na gramática (tira, suporte, rack, baia, carimbo); os textos são pt-BR simples.

O sistema existe em dois climas do mesmo lugar: escuro por padrão (cabine à noite, projetor em sala reduzida) e claro (sala com luz de leitura). O tema segue a preferência do sistema até o usuário escolher; a escolha fica salva no navegador. Numerais e rótulos são condensados e firmes, dados de máquina (ID, horário) em monoespaçada. Tudo é reto, plano e alinhado por colunas compartilhadas: o rack é uma estrutura, não uma pilha de cartões. Movimento só comunica estado: a tira entra, a tira sobe ou desce, o carimbo cai no fechamento.

**Key Characteristics:**
- Um campo de cor (rack) e uma superfície de dado (papel marfim); nada mais compete.
- Estado dito três vezes: cor do suporte, padrão de borda da tira e rótulo de texto.
- Caneta vermelha reservada às marcas: carimbo, cruz de ultrapassada, aba dobrada de "onde parei".
- Plano, sem sombras; profundidade por camadas tonais e bordas de 1 a 2 px.
- Densidade de projeção: nenhum texto abaixo de 1 rem, numerais de líder em 4 rem.
- Modo Operate, pt-BR, WCAG AA nos dois temas, movimento respeitando `prefers-reduced-motion`.

## Colors

Paleta de um campo só: neutros esverdeados sob quase todo o pixel, uma tinta de papel e três cores de suporte que só aparecem como peça de estado. Os valores do frontmatter são normativos; o escuro é a base e o claro sobrepõe com o sufixo `-light`. Tokens sem variante `-light` (`pen`) têm o mesmo valor nos dois temas.

### Primary
- **Âmbar de Suporte** (`contend` #e0ab45 escuro / #b7791a claro): suporte da tira em disputa, sublinhado da navegação atual, cor de ação primária no escuro (`action-bg`), acento do marcador "conectando". Cor de estado e de convite à ação, nunca de superfície grande.

### Secondary
- **Azul de Líder** (`leader` #8db4e0 escuro / #2b5c97 claro): exclusivamente o suporte da tira líder e o da tira de arquivo com vencedor.
- **Cinza de Ultrapassada** (`passed` #8a948e escuro / #6d7872 claro): suporte da tira ultrapassada, da tira "não vencedora" após o fechamento e da tira de arquivo sem vencedor.

### Tertiary
- **Caneta Vermelha** (`pen` #b3271a nos dois temas): tinta sobre o papel: carimbo do vencedor, cruz da ultrapassada, aba dobrada da última acompanhada.
- **Caneta sobre Rack** (`pen-on-rack` #f08a78 escuro / #8f1d12 claro): a mesma caneta quando a marca cai direto no rack, sem papel (moldura do resultado do leilão).

### Neutral
- **Rack** (`rack` #1c2622 / #b4c1b8): fundo da página e da barra; o aparelho onde tudo se insere.
- **Rack Elevado** (`rack-2` #243029 / #c5d0c8): moldura do rack, painel da regra, campo de texto, hover de botão.
- **Linha do Rack** (`rack-line` #3a4941 / #8b9d92): bordas de botão, campo, moldura e divisor da barra. Divisória, não contorno de leitura.
- **Linha de Controle** (`control-line` #6d8177 / #4a5d52): contorno de botão e campo, com contraste de pelo menos 3:1 contra o rack e a moldura (WCAG 1.4.11). O sublinhado da navegação atual usa `accent-text`, não `contend`.
- **Tinta** (`ink` #e8eee9 / #131d18) e **Tinta Secundária** (`ink-2` #aebbb3 / #2b3a32): texto sobre o rack.
- **Papel Marfim** (`paper` #f2eedd / #fbf8ec) com **Tinta de Papel** (`paper-ink` #1a1e1c / #171b19) e **Tinta de Papel Secundária** (`paper-ink-2` #4f5651 / #4a514c): a tira e a tira de arquivo.
- **Borda da Tira** (`strip-edge` #0f1512 / #33403a): contorno da tira e do suporte.
- **Texto de Acento** (`accent-text` #e0ab45 / #6b4300): posição (1º, 2º, 3º) da regra, moldura de erro, selo de demonstração. Existe porque o âmbar de suporte não tem contraste de texto no claro.
- **Foco** (`focus` #f0c75e / #0b3d91), **Ação** (`action-bg` #e0ab45 / #16241c com `action-ink` #1a1e1c / #f4f0e0).
- **Estado de conexão** (`state-ok` #8fd19e / #1d6b34, `state-wait` #e0ab45 / #8a5a00, `state-off` #aebbb3 / #2b3a32): só no marcador da conexão.

### Named Rules
**The One Field Rule.** O rack é o único campo de cor. Azul, âmbar e cinza aparecem só como suporte de tira (peça pequena e inserida) e nunca como fundo de painel, botão de contexto ou texto corrido.

**The Pen Rule.** Vermelho é caneta: só marca sobre uma tira (carimbo, cruz, aba) ou o contorno do resultado. Não é cor de erro nem de perda; a mensagem de erro do campo usa borda de `accent-text`, não vermelho.

**The No Trading Board Rule.** Verde e vermelho nunca codificam subida e descida de taxa. O verde só existe no marcador de conexão.

## Typography

**Display, Headline, Title, Body, Label:** Barlow Condensed (com Arial Narrow, sans-serif), auto-hospedada via @fontsource, pesos 500, 600 e 700, subconjunto latino.
**Data:** monoespaçada do sistema (ui-monospace, SF Mono, Menlo, Consolas), para ID da proposta, horário e campo de ID.

**Character:** Condensada de família única para caber muito numeral em pouca largura sem gritar; a monoespaçada marca o que é dado bruto do sistema. Numerais tabulares em todo o corpo (`font-variant-numeric: tabular-nums`).

### Hierarchy
- **Display** (700, 4 rem, 1): taxa da tira líder. No celular, 3 rem.
- **Headline** (700, 2 rem, 1.1): título da vista (h1).
- **Title** (700, 1.5 rem, 1.1): subtítulos (h2), nome do financiador na tira, taxa das tiras não líderes, marca CaaS Leilão (caixa alta, 0,04 em), resultado do leilão.
- **Body** (500, 1.125 rem, 1.35): texto corrido, navegação, prazo. Textos de apoio em 1.0625 rem (`text-sm`).
- **Label** (600, 1 rem, 0,06 em, caixa alta quando é rótulo de estado ou cabeçalho de coluna): rótulo de estado da tira (Melhor lance, Em disputa, Ultrapassada, Vencedora, Não vencedora), trilho de colunas, selos de arquivo, selo de demonstração.
- **Data** (mono, 1 rem a 1.0625 rem): horário da tira, ID da proposta, texto digitado no campo.

### Named Rules
**The One Family Rule.** Uma condensada para tudo que é rótulo e numeral, uma monoespaçada para dado de máquina. Nenhuma terceira família.

**The Floor Rule.** Nada abaixo de 1 rem: a tela é lida projetada.

## Layout

Colunas compartilhadas definem a estrutura: `--cols: 1.5rem 2.5rem minmax(0, 1fr) 7.5rem 6.5rem 6.5rem` (suporte, posição, financiador, taxa, prazo, hora). Trilho, tiras e baias vazias usam as mesmas colunas, então números se alinham em coluna. O corpo tem duas colunas: painel da regra de 17,5 rem e coluna central de até 52 rem, separados por 2 rem, centrados. Histórico é uma coluna de até 52 rem.

Ritmo de espaçamento em escala fixa: 0,25 / 0,5 / 0,75 / 1 / 1,5 / 2 rem. Tiras separadas por 0,5 rem; padding da moldura 0,5 rem; padding da página 1,5 rem.

Abaixo de 52 rem: uma coluna; o trilho de colunas some; a tira passa a duas linhas (financiador e taxa em cima, prazo e hora embaixo) com a taxa do líder em 3 rem; o painel da regra vira uma linha compacta com as três prioridades e sem o parágrafo de apoio; a barra reduz padding e alvos de toque ficam em 2,25 rem.

## Elevation & Depth

Plano, sem nenhuma sombra. A profundidade é camada tonal: página (`rack`) → moldura do rack (`rack-2`, borda 1 px `rack-line`) → tira de papel (borda `strip-edge`). A elevação é declarada uma vez, na própria hierarquia de camadas, e a tira líder ganha destaque por altura (8 rem contra 4,5 rem), borda de 2 px e numeral de 4 rem, não por sombra.

### Named Rules
**The Flat Rack Rule.** Nenhum `box-shadow`, blur ou vidro. Profundidade é tom de fundo mais borda.

**The Declared Once Rule.** A única elevação do sistema é rack, moldura, papel. Nenhum componente cria uma quarta camada.

## Shapes

Cantos quase retos (2 px) em tira, suporte, botão, campo e tira de arquivo; cantos zero no carimbo, no selo de demonstração, no marcador de conexão, no painel da regra e na baia vazia. Bordas de 1 px são o padrão; 2 px marcam a líder e o resultado. O suporte é uma peça própria: inserido na borda esquerda da tira com margem de 0,375 rem em cima, embaixo e à esquerda, contorno `strip-edge` e sem encostar na borda da tira. A borda tracejada é vocabulário de estado (ultrapassada, baia vazia, modo demonstração, "reconectando", vazio do histórico).

**Estado por padrão de borda, além de cor:**
- Líder: borda sólida 2 px, suporte azul, tira alta.
- Em disputa: borda sólida 1 px, suporte âmbar.
- Ultrapassada: borda tracejada 1 px, texto secundário, suporte cinza, cruz de caneta no rótulo.
- Vencedora (fechamento): carimbo em caneta vermelha inclinado.
- Conexão: quadrado cheio (conectado), vazado (conectando), tracejado (reconectando), vazado cinza (desconectado).
Todo estado também tem rótulo de texto.

## Components

### Tira
Papel marfim de 4,5 rem de altura mínima, canto 2 px, borda 1 px `strip-edge`, colunas do rack. Da esquerda: suporte, posição (Nº, cinza de papel), financiador (Title) com rótulo de estado (Label) abaixo, taxa (Title, à direita), prazo em meses, hora em mono. Entrada com animação `strip-in` de 220 ms (sobe 0,5 rem e aparece). Quando a ordem muda, as tiras deslizam para a nova posição em 260 ms (Web Animations, mesma curva, dispensado com movimento reduzido). A tira líder é dupla: 8 rem, borda 2 px, taxa em Display. Após o fechamento, as tiras em disputa recebem suporte cinza e o rótulo "Não vencedora".

### Suporte
Peça de 1,5 rem de coluna, 1 px `strip-edge`, canto 2 px, fundo âmbar por padrão; azul na líder, cinza na ultrapassada. Decorativo (`aria-hidden`); o estado é dito pelo texto do rótulo.

### Rack, trilho e baia
A moldura (`rack-2`, borda 1 px, padding 0,5 rem) abriga o trilho de colunas (Label caixa alta: Pos., Financiador, Taxa, Prazo, Hora), a lista de tiras e as baias vazias: contornos tracejados de 1 px em `ink-2`, 3 rem, numeradas 1º..3º, que mostram as posições ainda livres. Vazio total: linha "Nenhum lance ainda. O rack está vazio."

### Carimbo
O rótulo da tira vencedora: borda 2 px, texto e contorno `pen`, inclinado -3°, canto zero, entrada `stamp-in` de 260 ms (escala 1,5 a 1, rotação de -3°). É o único momento autoral do fechamento. Junto, moldura de resultado com borda 2 px `pen-on-rack`, Title, em texto simples.

### Cruz de caneta
Dois traços diagonais de 1 px em `pen` (0,75 rem) antes do rótulo "Ultrapassada": a tira já liderou e foi superada.

### Tira de arquivo
Tira de papel de 3,5 rem, mesma linguagem, suporte azul (vencedor visto), cinza (sem vencedor) ou âmbar (desfecho não visto). ID em mono como link, desfecho em Body 700, selos "Último acompanhado" e "Simulado" em Label. A última acompanhada recebe a aba dobrada: triângulo de 1,25 rem no canto superior direito, metade `rack`, metade `pen`. No celular, quatro linhas empilhadas ao lado do suporte.

### Barra do shell
Faixa fina no topo, borda inferior 1 px `rack-line`, padding 0,75 rem por 1,5 rem, quebra em linhas no celular. Marca CaaS Leilão (Title, caixa alta, 0,04 em), navegação Ao vivo e Histórico (Body, `ink-2`; atual em `ink` com sublinhado de 2 px em `contend`), à direita: selo "Modo demonstração" (Label caixa alta, borda tracejada 1 px em `accent-text`), estado da conexão em texto com marcador quadrado de 0,75 rem, links Trace e Métricas (fora da demonstração) e botão de tema.

### Botões
- **Forma:** canto 2 px, altura mínima 2,5 rem, padding horizontal 1 rem, borda 1 px `rack-line`.
- **Padrão:** fundo transparente, texto `ink`, peso 600. Hover: fundo `rack-2` em 150 ms. Ativo: desce 1 px. Desabilitado: 50% de opacidade.
- **Primário (Acompanhar):** fundo `action-bg`, texto `action-ink`. Hover: brilho 1,08.
- **Foco:** contorno de 2 px em `focus`, deslocado 2 px, em todo elemento focável.

### Campo
ID da proposta: altura 2,5 rem, canto 2 px, borda 1 px `rack-line`, fundo `rack-2`, texto mono 1.0625 rem, placeholder `ink-2`. Rótulo visível acima (Body 600, `ink-2`). Erro: mensagem em bloco com borda 1 px `accent-text` e `role="alert"`, sem cor de erro dedicada. Foco como nos botões.

## Do's and Don'ts

### Do:
- **Do** dizer o estado de três formas: suporte colorido, padrão de borda e rótulo de texto.
- **Do** manter a regra de desempate impressa ao lado do rack, com as três prioridades numeradas.
- **Do** alinhar trilho, tiras e baias pelas mesmas colunas (`--cols`).
- **Do** usar caneta vermelha só como marca sobre uma tira: carimbo, cruz e aba.
- **Do** limitar movimento a estado (entrada, reordenação, carimbo) com `--ease-out` e desligá-lo em `prefers-reduced-motion: reduce`.
- **Do** manter texto de leitura em 1 rem ou mais e usar `accent-text` (não `contend`) para texto âmbar.
- **Do** assinalar o modo demonstração com o selo tracejado na barra.

### Don't:
- **Don't** usar eyebrow, kicker ou rótulo decorativo acima de títulos.
- **Don't** repetir cartões iguais soltos sobre neutro: a estrutura é o rack com colunas, não uma grade de cartões.
- **Don't** usar borda lateral colorida grossa como acento; a cor de estado vive no suporte, peça inserida com margem e contorno próprios.
- **Don't** usar listras, faixas ou padrões decorativos.
- **Don't** usar sombra (dura ou difusa), vidro, blur ou gradiente de texto.
- **Don't** usar verde e vermelho como placar de subida e descida, nem vermelho como cor de erro.
- **Don't** aninhar cartões dentro de cartões.
- **Don't** mostrar contagem regressiva, barra de progresso de prazo ou qualquer dado que o sistema não fornece.
- **Don't** carregar fontes ou ativos de terceiros em execução; a fonte é auto-hospedada.
- **Don't** introduzir uma terceira família tipográfica nem cor de suporte fora de azul, âmbar e cinza.
