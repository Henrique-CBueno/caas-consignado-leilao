# Domain Docs

Como as skills de engenharia devem consumir a documentação de domínio deste repo ao explorar o código.

## Antes de explorar, ler isto

- **`CONTEXT.md`** na raiz do repo (ainda não existe — criado sob demanda pela skill `domain-modeling` quando termos/decisões forem resolvidos; não crie antecipadamente).
- **`docs/adr/`**: ler as ADRs que tocam a área em que você vai trabalhar.

Se algum desses arquivos não existir, **prossiga em silêncio**. Não sinalize a ausência; não sugira criá-los antecipadamente.

## Estrutura de arquivos

Repositório single-context (este repo, apesar de ter múltiplos microsserviços em `services/`, usa um único `CONTEXT.md` e uma única `docs/adr/` na raiz — não há sinais de monorepo multi-pacote no sentido que esta skill define, como `pnpm-workspace.yaml`):

```
/
├── CONTEXT.md          (a criar sob demanda)
├── docs/adr/
│   └── 0001-monorepo-gradle-multimodulo.md
└── services/, dashboard/, infra/, libs/
```

## Use o vocabulário do glossário

Quando a saída nomear um conceito de domínio (título de issue, proposta de refactor, hipótese, nome de teste), use o termo como definido em `CONTEXT.md`. Não desvie para sinônimos que o glossário evita explicitamente.

Se o conceito ainda não estiver no glossário, isso é um sinal: ou é linguagem inventada que o projeto não usa (reconsiderar), ou é uma lacuna real (anotar para `/domain-modeling`).

## Sinalizar conflitos com ADR

Se a saída contradiz uma ADR existente, sinalize explicitamente em vez de sobrepor silenciosamente.
