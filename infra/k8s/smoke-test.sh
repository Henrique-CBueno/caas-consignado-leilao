#!/bin/sh
set -e

TENANT_ID=$(cat /proc/sys/kernel/random/uuid)
echo "tenant: $TENANT_ID"

# traceparent conhecido: permite buscar exatamente o trace desta proposta no Jaeger depois.
TRACE_ID=$(cat /proc/sys/kernel/random/uuid | tr -d '-')
SPAN_ID=$(cat /proc/sys/kernel/random/uuid | tr -d '-' | cut -c1-16)
echo "trace: $TRACE_ID"

fail() { echo "$1"; exit 1; }
PROPOSAL_JSON='{"borrowerId":"59","requestedAmount":5000.00,"termMonths":24}'

if [ "$VIA_GATEWAY" = "1" ]; then
  # Deploy completo: tudo pela borda pública, com tokens reais do cognito-local (ADR-0017).
  COGNITO=http://cognito-local:9229
  GW=http://api-gateway:8080
  TENANT_B=22222222-2222-2222-2222-222222222222
  cognito() {
    curl -s -X POST "$COGNITO" -H 'Content-Type: application/x-amz-json-1.1' \
      -H "X-Amz-Target: AWSCognitoIdentityProviderService.$1" -d "$2"
  }
  # O emulador só tem o pool "caas" (criado pelo bootstrap): o único "Id" da resposta é o do pool.
  POOL_ID=$(cognito ListUserPools '{"MaxResults":10}' | sed -n 's/.*"Id":"\([^"]*\)".*/\1/p')
  CLIENT_ID=$(cognito ListUserPoolClients "{\"UserPoolId\":\"$POOL_ID\",\"MaxResults\":10}" | sed -n 's/.*"ClientId":"\([^"]*\)".*/\1/p')
  token_for() {
    cognito AdminInitiateAuth "{\"UserPoolId\":\"$POOL_ID\",\"ClientId\":\"$CLIENT_ID\",\"AuthFlow\":\"ADMIN_USER_PASSWORD_AUTH\",\"AuthParameters\":{\"USERNAME\":\"$1\",\"PASSWORD\":\"Passw0rd1!\"}}" \
      | sed -n 's/.*"IdToken":"\([^"]*\)".*/\1/p'
  }
  TOKEN_A=$(token_for alfa@caas.local)
  TOKEN_B=$(token_for beta@caas.local)
  [ -n "$TOKEN_A" ] && [ -n "$TOKEN_B" ] || fail "AUTH FAILED: sem tokens do cognito-local"
  echo "tokens obtidos (tenants alfa e beta)"

  # Cria como tenant A enviando um X-Tenant-Id forjado (tenant B): o gateway deve ignorá-lo.
  CREATE_RESPONSE=$(curl -s -X POST "$GW/proposals" \
    -H "Authorization: Bearer $TOKEN_A" -H "X-Tenant-Id: $TENANT_B" \
    -H "Content-Type: application/json" -H "traceparent: 00-$TRACE_ID-$SPAN_ID-01" -d "$PROPOSAL_JSON")
else
  CREATE_RESPONSE=$(curl -s -X POST http://proposal-service:8080/proposals \
    -H "Content-Type: application/json" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "traceparent: 00-$TRACE_ID-$SPAN_ID-01" \
    -d "$PROPOSAL_JSON")
fi
echo "proposal created: $CREATE_RESPONSE"

PROPOSAL_ID=$(echo "$CREATE_RESPONSE" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
echo "proposalId: $PROPOSAL_ID"
[ -n "$PROPOSAL_ID" ] || fail "FALHA: proposta não criada"

if [ "$VIA_GATEWAY" = "1" ]; then
  # Isolamento: o dono (A) lê; o tenant B não enxerga (RLS). Se o header forjado valesse, A receberia 404.
  A_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$GW/proposals/$PROPOSAL_ID" -H "Authorization: Bearer $TOKEN_A")
  B_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$GW/proposals/$PROPOSAL_ID" -H "Authorization: Bearer $TOKEN_B")
  [ "$A_STATUS" = "200" ] || fail "ISOLAMENTO FALHOU: dono recebeu $A_STATUS (header forjado valeu?)"
  [ "$B_STATUS" = "404" ] || fail "ISOLAMENTO FALHOU: outro tenant recebeu $B_STATUS (esperado 404)"
  echo "ISOLAMENTO OK: header forjado ignorado; dono=$A_STATUS, outro tenant=$B_STATUS"

  NOTOKEN_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$GW/proposals/$PROPOSAL_ID")
  [ "$NOTOKEN_STATUS" = "401" ] || fail "AUTH FALHOU: sem token recebeu $NOTOKEN_STATUS (esperado 401)"
  echo "AUTH OK: sem token = 401"

  # Este pod não é o gateway: a NetworkPolicy deve barrar o acesso direto ao serviço interno.
  if curl -s --max-time 5 -o /dev/null http://proposal-service:8080/actuator/health; then
    fail "NETPOLICY FALHOU: pod fora do gateway alcançou o proposal-service"
  fi
  echo "NETPOLICY OK: acesso direto ao proposal-service bloqueado"
fi

echo "waiting for disbursement..."
for i in $(seq 1 60); do
  if [ "$VIA_GATEWAY" = "1" ]; then
    RESPONSE=$(curl -s -o /tmp/body -w "%{http_code}" "$GW/disbursements/$PROPOSAL_ID" -H "Authorization: Bearer $TOKEN_A")
  else
    RESPONSE=$(curl -s -o /tmp/body -w "%{http_code}" "http://disbursement-service:8080/disbursements/$PROPOSAL_ID" -H "X-Tenant-Id: $TENANT_ID")
  fi
  if [ "$RESPONSE" = "200" ]; then
    echo "DISBURSEMENT FOUND:"
    cat /tmp/body
    echo ""
    FOUND_DISBURSEMENT=1
    break
  fi
  sleep 3
done

if [ "$FOUND_DISBURSEMENT" != "1" ]; then
  echo "TIMEOUT: disbursement never appeared"
  exit 1
fi

# Verificação de observabilidade (Milestone 13) — só no deploy completo; o modo slim
# do CI não sobe Jaeger/Prometheus/Grafana (ADR-0009/0011).
if [ "$CHECK_OBSERVABILITY" = "1" ]; then
  EXPECTED="auction-service contract-service credit-analysis-service disbursement-service funder-bot-service proposal-service"
  for i in $(seq 1 30); do
    curl -s "http://jaeger:16686/api/traces/$TRACE_ID" -o /tmp/trace || true
    FOUND=$(grep -o '"serviceName":"[^"]*"' /tmp/trace | sed 's/.*:"\(.*\)"/\1/' | sort -u)
    MISSING=""
    for svc in $EXPECTED; do
      echo "$FOUND" | grep -qx "$svc" || MISSING="$MISSING $svc"
    done
    [ -z "$MISSING" ] && break
    sleep 3
  done
  if [ -n "$MISSING" ]; then
    echo "TRACE INCOMPLETE ($TRACE_ID): faltam:$MISSING | encontrados: $(echo $FOUND)"
    exit 1
  fi
  echo "TRACE OK ($TRACE_ID): $(echo $FOUND)"

  for i in $(seq 1 30); do
    curl -s http://prometheus:9090/api/v1/targets -o /tmp/targets || true
    UP=$(grep -o '"health":"up"' /tmp/targets | wc -l)
    DOWN=$(grep -o '"health":"down"' /tmp/targets | wc -l)
    [ "$UP" -ge 9 ] && [ "$DOWN" -eq 0 ] && break
    sleep 5
  done
  if [ "$UP" -lt 9 ] || [ "$DOWN" -ne 0 ]; then
    echo "PROMETHEUS TARGETS: up=$UP down=$DOWN (esperado: up>=9, down=0)"
    exit 1
  fi
  echo "PROMETHEUS OK: $UP alvos up"

  for i in $(seq 1 20); do
    curl -s "http://grafana:3000/api/search?query=CaaS%20Overview" -o /tmp/dash || true
    grep -q '"title":"CaaS Overview"' /tmp/dash && break
    sleep 3
  done
  grep -q '"title":"CaaS Overview"' /tmp/dash || { echo "GRAFANA: dashboard 'CaaS Overview' não provisionado"; exit 1; }
  echo "GRAFANA OK: dashboard 'CaaS Overview' provisionado"
fi

exit 0
