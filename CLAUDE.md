# CaaS Consignado Leilão

Projeto de portfólio técnico: plataforma simulada de Credit-as-a-Service para leilão reverso de crédito consignado. Ver `README.md` e o plano de implementação completo para arquitetura, milestones e ADRs.

## Agent skills

### Issue tracker

Issues e specs vivem como GitHub issues em [Henrique-CBueno/caas-consignado-leilao](https://github.com/Henrique-CBueno/caas-consignado-leilao) (privado). Ver `docs/agents/issue-tracker.md`.

### Domain docs

Layout single-context: `CONTEXT.md` (glossário do domínio) + `docs/adr/` (índice em `docs/adr/README.md`) na raiz. Ver `docs/agents/domain.md`.

### Documentação

`make docs-check` valida links, estrutura dos ADRs e alvos `make` citados; `make test` cobre a deriva das OpenAPI em `docs/openapi/` (regenerar com `./gradlew contractTest -Dopenapi.update=true`). Diagramas em `docs/c4/`, roteiro de demo em `docs/demo.md`.
