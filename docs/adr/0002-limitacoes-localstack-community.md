# 0002 — Limitações do LocalStack Community: RDS e Cognito não emulados

## Status
Aceita

## Contexto
O plano original assumia Postgres via RDS emulado e autenticação via Cognito emulado, ambos provisionados por Terraform contra o LocalStack, com o Risco #1 e #2 do plano já prevendo que essas emulações pudessem ter limitações (ex.: Cognito sem Hosted UI/MFA no Community). Validação empírica feita na Milestone 1 mostrou que a realidade é mais severa: **nenhum dos dois serviços é implementado, nem parcialmente**, no LocalStack Community — ambos retornam `501` com a mensagem `"API for service '<rds|cognito-idp>' not yet implemented or pro feature"`. Já **DynamoDB** e (presumivelmente, por ser um dos serviços mais maduros do projeto) **S3** funcionam normalmente no Community.

## Decisão
1. **Postgres**: nunca via RDS/LocalStack. Um container Postgres simples (`postgres:16-alpine`) roda diretamente (`docker-compose.dev.yml` no loop de dev; um `Deployment`/`StatefulSet` próprio no minikube na Milestone 9) — sem Terraform envolvido nessa peça, já que não há recurso AWS real por trás.
2. **Autenticação**: nunca via Cognito/LocalStack. Usar **cognito-local** (`jagregory/cognito-local`, emulador dedicado e gratuito) como container/pod próprio, fora do LocalStack. Ele implementa `CreateUserPool`, `CreateUserPoolClient`, `AdminCreateUser`, `AdminSetUserPassword` e `AdminInitiateAuth` (com `ADMIN_USER_PASSWORD_AUTH`, não `ADMIN_NO_SRP_AUTH`, que não é suportado) pela mesma API JSON do Cognito real, e assina JWTs de verdade com JWKS real em `/{userPoolId}/.well-known/jwks.json`. Como não é um serviço LocalStack, **não é provisionado via Terraform** — o user pool/client são criados chamando a própria API do cognito-local (script de bootstrap chama a mesma sequência de chamadas usada nos testes de integração do `api-gateway`).
3. **LocalStack** continua existindo no projeto, mas seu escopo real é só **DynamoDB e S3** — `SERVICES=dynamodb,s3` no `docker-compose.dev.yml`, e é o único par (DynamoDB/S3) que efetivamente faz sentido continuar gerenciando via Terraform.
4. **Terraform**: o módulo `postgres-rds` e o módulo `cognito` descritos no plano original são **removidos/nunca implementados**; só existem módulos Terraform para o que o LocalStack realmente emula (DynamoDB, S3).

## Consequências
- A narrativa "tudo AWS-native simulado via Terraform+LocalStack" fica mais honesta: nem todo componente de infra é um recurso AWS de fato, e isso é documentado explicitamente em vez de forçado.
- `cognito-local` precisa de seu próprio bootstrap (script chamando a API JSON diretamente), não `terraform apply`. Isso é aceitável e explicado ao leitor via esta ADR — parte do valor de portfólio é justamente saber reconhecer e documentar esse tipo de limitação real, não escondê-la.
- Na Milestone 9 (Kubernetes), tanto o Postgres quanto o cognito-local entram como workloads próprios no minikube (Deployment ou StatefulSet + Service), ao lado de Kafka/Zookeeper/Vault — LocalStack continua só para DynamoDB/S3.
- O `TokenValidityUnits`/claims exatos emitidos pelo cognito-local podem divergir sutilmente do Cognito real (ex.: formato do `iss`, que aponta para o próprio host:porta do cognito-local, não para um domínio `cognito-idp.<region>.amazonaws.com`); isso é aceitável para fins de demo/portfólio e fica registrado aqui como limitação conhecida.
