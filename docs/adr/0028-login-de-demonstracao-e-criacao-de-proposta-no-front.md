# 0028 — Login de demonstração e criação de proposta no front

## Status
Aceita

## Contexto
Criar proposta exigia terminal (`make token` + `curl`). O gateway só aceita JWT com o claim de tenant (ADR-0017), o front era público e sem login, o `cognito-local` é `ClusterIP` (inacessível do navegador) e o gateway não tinha CORS.

## Decisão
- **`POST /auth/login` no `api-gateway`** (pública, sem JWT): recebe `{"tenant":"alfa|beta|gama"}`, rejeita qualquer outro valor com 400 antes de falar com o Cognito e autentica pelo fluxo **público** `InitiateAuth`/`USER_PASSWORD_AUTH` (nunca `AdminInitiateAuth`), com a senha de demonstração fixa (ADR-0017). Devolve `{"idToken"}`. O `cognito-local` continua só acessível de dentro do cluster.
- **`ClientId` por configuração** (`APP_COGNITO_CLIENT_ID`, escrito pelo `make deploy-local` junto do JWKS): `InitiateAuth` não precisa do `UserPoolId`, então não há descoberta em runtime. Achado real: o `cognito-local` **exige** `Content-Type: application/x-amz-json-1.1` na requisição (com `application/json` responde 500), e o `WebClient` não tem encoder para esse tipo, por isso o corpo é serializado à mão em bytes e a resposta lida como texto.
- **CORS** só em `/auth/**` e `/proposals/**` (origem `*` na Milestone 16, restrita à origem do dashboard na Milestone 18 — [ADR-0030](0030-websocket-autenticado-por-tenant-e-cors-restrito.md) —, métodos GET/POST, cabeçalhos `Authorization` e `Content-Type`). Limitação assumida de demonstração: a proteção continua sendo o JWT depois do preflight.
- **Front**: `AuthService` injetável (`AUTH`, mesmo padrão de `AuctionFeed`), sessão em `sessionStorage` (não sobrevive a fechar a aba); telas **Entrar** (seletor dos 3 tenants, sem campo de senha) e **Nova proposta** (valida antes de enviar, envia com o Bearer e navega para `?proposta=<id>`); cabeçalho mostra o tenant logado e "Sair". Ao vivo e Histórico continuam públicos.

## Consequências
- O fluxo completo (entrar, criar proposta, acompanhar o leilão) cabe no navegador; validado no cluster real.
- `make deploy-local` deve recriar o cluster do zero ao trocar imagens `:local`: depois de `minikube stop/start` o `image load` **não substituiu** a imagem antiga (imagem em uso não pode ser removida); solução usada: escalar o deployment a 0, `minikube image rm`, `image load`, escalar a 1.
- Fora de escopo (Milestone 17): painel de administrador de tenants, que exige um papel administrativo que atravessa a RLS.
