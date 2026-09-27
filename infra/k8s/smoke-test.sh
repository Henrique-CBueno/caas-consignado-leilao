#!/bin/sh
set -e

TENANT_ID=$(cat /proc/sys/kernel/random/uuid)
echo "tenant: $TENANT_ID"

CREATE_RESPONSE=$(curl -s -X POST http://proposal-service:8080/proposals \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $TENANT_ID" \
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
    exit 0
  fi
  sleep 3
done

echo "TIMEOUT: disbursement never appeared"
exit 1
