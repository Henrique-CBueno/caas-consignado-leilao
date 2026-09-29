# 0029 — Papel administrativo e painel de tenants

## Status
Aceita

## Contexto
Nenhuma sessão da aplicação enxerga mais de um tenant: a política de RLS de `tenants` só libera a linha do tenant do contexto e `app_role` é `NOBYPASSRLS` (ADR-0014). Não havia como listar ou cadastrar tenants, nem conceito de papel: todo usuário autenticado pertencia a um tenant.

## Decisão
- **Identidade `admin`**: usuário demo `admin@caas.local` no Cognito emulado com o claim `custom:role=admin` e **sem** claim de tenant (o `bootstrap.sh` cria o atributo `role` no pool). A rota `/auth/login` passou a aceitar qualquer slug `[a-z0-9-]+` (inclui `admin` e os tenants criados depois do seed); quem decide se o usuário existe é o Cognito, e o erro vira 400.
- **Gateway**: `/admin/**` roteia para o `tenant-service` e só passa com `custom:role=admin` (senão 403). O papel vem só do JWT validado, nunca de header do cliente. A identidade admin **não** ganha dados de tenant: sem claim de tenant, `/proposals/**` e `/disbursements/**` continuam respondendo 403. O rate limit da identidade admin tem chave própria.
- **`admin_role` no Postgres** (`V5`): `BYPASSRLS`, `NOSUPERUSER`, `NOLOGIN`, com **apenas** `SELECT` e `INSERT` em `tenants` (nunca `UPDATE`/`DELETE`, coberto por teste). Só o `JpaAdminTenantRepository` a assume (`SET LOCAL ROLE`); o caminho por tenant (`JpaTenantRepository`) não mudou e um teste prova que a sessão de tenant continua vendo só a própria linha. Índice único em `tenants.name`.
- **`POST /admin/tenants`** cria a linha e, pela porta `DemoUserProvisioner` (adaptador HTTP ao `cognito-local`), o usuário `<slug>@caas.local` com o claim do novo tenant e a senha demo fixa (ADR-0017). Nome vazio dá 400, repetido 409. Se o Cognito falhar, a resposta é 502 explícita: o tenant já foi gravado (sem transação distribuída), então fica listado sem usuário demo.
- **Front**: a tela Entrar ganha "Entrar como administrador" e passa a oferecer também os tenants criados neste navegador (`localStorage`); não existe listagem pública de tenants, porque vazaria os nomes dos bancos. A vista **Administração** (`/admin`) lista e cria tenants e só aparece para a sessão admin.

## Consequências
- Fluxo completo de demo: entrar como admin, criar um banco, entrar como ele e criar uma proposta.
- `BYPASSRLS` é poder real; por isso mínimo (só ler/inserir) e restrito a um único caminho de código.
- Limitações: um usuário demo por tenant com senha fixa; o seletor de entrada só conhece os tenants criados no mesmo navegador; um pool do `cognito-local` criado antes desta milestone não tem o atributo `role` (recriar o cluster).
- Fora de escopo: editar/apagar/desativar tenant, outros papéis, acesso do admin a dados de tenant.
