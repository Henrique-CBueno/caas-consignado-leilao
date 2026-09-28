#!/bin/sh
set -e

TENANT_ID=$(cat /proc/sys/kernel/random/uuid)
echo "tenant: $TENANT_ID"

# traceparent conhecido: permite buscar exatamente o trace desta proposta no Jaeger depois.
TRACE_ID=$(cat /proc/sys/kernel/random/uuid | tr -d '-')
SPAN_ID=$(cat /proc/sys/kernel/random/uuid | tr -d '-' | cut -c1-16)
echo "trace: $TRACE_ID"

CREATE_RESPONSE=$(curl -s -X POST http://proposal-service:8080/proposals \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "traceparent: 00-$TRACE_ID-$SPAN_ID-01" \
  -d '{"borrowerId":"59","requestedAmount":5000.00,"termMonths":24}')
echo "proposal created: $CREATE_RESPONSE"

PROPOSAL_ID=$(echo "$CREATE_RESPONSE" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
echo "proposalId: $PROPOSAL_ID"

echo "waiting for disbursement..."
for i in $(seq 1 60); do
  RESPONSE=$(curl -s -o /tmp/body -w "%{http_code}" "http://disbursement-service:8080/disbursements/$PROPOSAL_ID" -H "X-Tenant-Id: $TENANT_ID")
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
