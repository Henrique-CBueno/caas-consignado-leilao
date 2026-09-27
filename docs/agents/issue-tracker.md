# Issue tracker: GitHub

Issues e specs deste repo vivem como GitHub issues, em [Henrique-CBueno/caas-consignado-leilao](https://github.com/Henrique-CBueno/caas-consignado-leilao) (privado). Use a CLI `gh` para todas as operações.

## Convenções

- **Criar uma issue**: `gh issue create --title "..." --body "..."`. Use heredoc para corpos multi-linha.
- **Ler uma issue**: `gh issue view <numero> --comments`, filtrando comentários com `jq` e também buscando labels.
- **Listar issues**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` com `--label`/`--state` apropriados.
- **Comentar numa issue**: `gh issue comment <numero> --body "..."`
- **Aplicar/remover labels**: `gh issue edit <numero> --add-label "..."` / `--remove-label "..."`
- **Fechar**: `gh issue close <numero> --comment "..."`

O repo é inferido de `git remote -v`; `gh` faz isso automaticamente dentro do clone.

## Pull requests como superfície de triagem

**PRs como superfície de request: não.** _(Mudar para "sim" só se este repo tratar PRs externos como feature requests — não é o caso, é um projeto solo.)_

## Quando uma skill disser "publicar no issue tracker"

Criar uma issue no GitHub.

## Quando uma skill disser "buscar o ticket relevante"

Rodar `gh issue view <numero> --comments`.
