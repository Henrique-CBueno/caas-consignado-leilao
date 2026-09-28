#!/bin/sh
# Cria (de forma idempotente) o pool, o client e um usuário por tenant de seed no cognito-local.
# Imprime "POOL_ID=<id>" na última linha, lido pelo Makefile para configurar o JWKS do gateway.
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

# O emulador só tem o pool "caas": o único "Id" da resposta de ListUserPools é o do pool.
POOL_ID=$(call ListUserPools '{"MaxResults":10}' | id_of)
if [ -z "$POOL_ID" ]; then
  POOL_ID=$(call CreateUserPool '{"PoolName":"caas","Schema":[{"Name":"tenant_id","AttributeDataType":"String","Mutable":true}]}' | id_of)
  call CreateUserPoolClient "{\"UserPoolId\":\"$POOL_ID\",\"ClientName\":\"caas-client\",\"ExplicitAuthFlows\":[\"ALLOW_ADMIN_USER_PASSWORD_AUTH\",\"ALLOW_REFRESH_TOKEN_AUTH\"]}" >/dev/null
fi

create_user() {
  # $1 = usuário, $2 = tenant (UUID do seed do tenant-service)
  call AdminCreateUser "{\"UserPoolId\":\"$POOL_ID\",\"Username\":\"$1\",\"TemporaryPassword\":\"Temp1234!\",\"MessageAction\":\"SUPPRESS\",\"UserAttributes\":[{\"Name\":\"custom:tenant_id\",\"Value\":\"$2\"}]}" >/dev/null
  call AdminSetUserPassword "{\"UserPoolId\":\"$POOL_ID\",\"Username\":\"$1\",\"Password\":\"Passw0rd1!\",\"Permanent\":true}" >/dev/null
}

create_user alfa@caas.local 11111111-1111-1111-1111-111111111111
create_user beta@caas.local 22222222-2222-2222-2222-222222222222
create_user gama@caas.local 33333333-3333-3333-3333-333333333333

echo "POOL_ID=$POOL_ID"
