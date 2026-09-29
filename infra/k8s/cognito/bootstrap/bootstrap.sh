#!/bin/sh
# Cria (de forma idempotente) o pool, o client e um usuário por tenant de seed no cognito-local.
# Imprime "POOL_ID=<id>" e "CLIENT_ID=<id>" nas últimas linhas, lidos pelo Makefile para
# configurar o gateway (JWKS e o client usado pela rota de login, Milestone 16).
# Senha de demonstração (não é segredo real): Passw0rd1!
set -e

COGNITO=http://cognito-local:9229

call() {
  curl -s -X POST "$COGNITO" \
    -H 'Content-Type: application/x-amz-json-1.1' \
    -H "X-Amz-Target: AWSCognitoIdentityProviderService.$1" \
    -d "$2"
}
id_of() { sed -n 's/.*"Id":"\([^"]*\)".*/\1/p'; }
client_id_of() { sed -n 's/.*"ClientId":"\([^"]*\)".*/\1/p'; }

# O emulador só tem o pool "caas": o único "Id" da resposta de ListUserPools é o do pool.
POOL_ID=$(call ListUserPools '{"MaxResults":10}' | id_of)
if [ -z "$POOL_ID" ]; then
  POOL_ID=$(call CreateUserPool '{"PoolName":"caas","Schema":[{"Name":"tenant_id","AttributeDataType":"String","Mutable":true},{"Name":"role","AttributeDataType":"String","Mutable":true}]}' | id_of)
  # ALLOW_USER_PASSWORD_AUTH: fluxo público (sem credencial de administrador), usado pela rota
  # de login do gateway (Milestone 16) — os demais fluxos seguem servindo make token/smoke-test.
  CLIENT_ID=$(call CreateUserPoolClient "{\"UserPoolId\":\"$POOL_ID\",\"ClientName\":\"caas-client\",\"ExplicitAuthFlows\":[\"ALLOW_ADMIN_USER_PASSWORD_AUTH\",\"ALLOW_USER_PASSWORD_AUTH\",\"ALLOW_REFRESH_TOKEN_AUTH\"]}" | client_id_of)
else
  CLIENT_ID=$(call ListUserPoolClients "{\"UserPoolId\":\"$POOL_ID\",\"MaxResults\":10}" | client_id_of)
fi

create_user() {
  # $1 = usuário, $2 = tenant (UUID do seed do tenant-service)
  call AdminCreateUser "{\"UserPoolId\":\"$POOL_ID\",\"Username\":\"$1\",\"TemporaryPassword\":\"Temp1234!\",\"MessageAction\":\"SUPPRESS\",\"UserAttributes\":[{\"Name\":\"custom:tenant_id\",\"Value\":\"$2\"}]}" >/dev/null
  call AdminSetUserPassword "{\"UserPoolId\":\"$POOL_ID\",\"Username\":\"$1\",\"Password\":\"Passw0rd1!\",\"Permanent\":true}" >/dev/null
}

create_user alfa@caas.local 11111111-1111-1111-1111-111111111111
create_user beta@caas.local 22222222-2222-2222-2222-222222222222
create_user gama@caas.local 33333333-3333-3333-3333-333333333333

# Administrador da plataforma (Milestone 17, ADR-0029): claim de papel, sem tenant.
call AdminCreateUser "{\"UserPoolId\":\"$POOL_ID\",\"Username\":\"admin@caas.local\",\"TemporaryPassword\":\"Temp1234!\",\"MessageAction\":\"SUPPRESS\",\"UserAttributes\":[{\"Name\":\"custom:role\",\"Value\":\"admin\"}]}" >/dev/null
call AdminSetUserPassword "{\"UserPoolId\":\"$POOL_ID\",\"Username\":\"admin@caas.local\",\"Password\":\"Passw0rd1!\",\"Permanent\":true}" >/dev/null

echo "POOL_ID=$POOL_ID"
echo "CLIENT_ID=$CLIENT_ID"
