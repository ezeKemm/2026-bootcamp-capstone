#!/usr/bin/env bash
# Smoke test after a deploy: is the application useful, not just running? (Lab 51 Step 6; Lab 41 "a listening port is not readiness".)
#   1 pods ready          2 the Angular app is served        3 guest list through the Ingress shows the seeded customer
#   4 no token gives 401  5 login, then read the seeded customer with the correlation id lab-request-001, echoed back
#   6 the API log carries corr=lab-request-001            7 /actuator is NOT reachable from outside     8 resource use (evidence)
# Needs KUBECONFIG, K8S_NAMESPACE, BASE_URL (the environment's PUBLIC_ORIGIN) and CRM_AGENT_PASSWORD. Needs curl and jq.
# The seeded customer is Amina Khan; her id is the fixed UUID from V2__seed_demo_customers.sql (CUS-1001 is only a label).
set -uo pipefail
set +x
: "${K8S_NAMESPACE:?}" "${BASE_URL:?}" "${CRM_AGENT_PASSWORD:?}"
command -v jq >/dev/null || { echo "jq is required" >&2; exit 2; }
BASE_URL="${BASE_URL%/}"
CUSTOMER_ID="5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11"
CORR="lab-request-001"
fails=0
pass() { echo "PASS  $1"; summary "- PASS $1"; }
fail() { echo "FAIL  $1" >&2; summary "- FAIL $1"; fails=$((fails + 1)); }
summary() { if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then echo "$@" >> "$GITHUB_STEP_SUMMARY"; fi; }
http() { curl -sS --max-time 20 --retry 8 --retry-delay 4 --retry-connrefused --retry-all-errors "$@"; }
summary "### Smoke test ($BASE_URL)"

# 1
if kubectl --namespace "$K8S_NAMESPACE" wait --for=condition=Ready pod -l app.kubernetes.io/part-of=northstar-crm --timeout=120s >/dev/null; then
  pass "all pods ready"
else fail "pods are not all ready"; fi

# 2
code="$(http -o /tmp/smoke-index.html -w '%{http_code}' "$BASE_URL/" || true)"
if [ "$code" = "200" ] && grep -qi '<html' /tmp/smoke-index.html; then pass "web app served (200)"; else fail "web app: HTTP $code"; fi

# 3
body="$(http "$BASE_URL/api/v1/public/customers" || true)"
if echo "$body" | grep -q 'Amina Khan'; then pass "guest list through the Ingress shows Amina Khan"; else fail "guest list does not show Amina Khan"; fi

# 4
code="$(http -o /dev/null -w '%{http_code}' "$BASE_URL/api/v1/customers" || true)"
if [ "$code" = "401" ]; then pass "no token gives 401"; else fail "no token gave HTTP $code, expected 401"; fi

# 5
token="$(http -X POST "$BASE_URL/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d "$(jq -n --arg p "$CRM_AGENT_PASSWORD" '{username:"agent1",password:$p}')" | jq -r '.accessToken // empty' || true)"
if [ -n "$token" ]; then
  pass "agent login returned a token"
  headers="$(mktemp)"
  code="$(http -D "$headers" -o /tmp/smoke-customer.json -w '%{http_code}' "$BASE_URL/api/v1/customers/$CUSTOMER_ID" \
    -H "Authorization: Bearer $token" -H "X-Correlation-Id: $CORR" || true)"
  if [ "$code" = "200" ] && grep -q 'Amina Khan' /tmp/smoke-customer.json; then pass "customer $CUSTOMER_ID read with a token"; else fail "customer read: HTTP $code"; fi
  if grep -qi "^x-correlation-id: $CORR" "$headers"; then pass "response echoes X-Correlation-Id: $CORR"; else fail "X-Correlation-Id was not echoed"; fi
  rm -f "$headers"
else
  fail "agent login did not return a token"
fi

# 6
if kubectl --namespace "$K8S_NAMESPACE" logs deployment/crm-api --since=10m 2>/dev/null | grep -q "corr=$CORR"; then
  pass "API log carries corr=$CORR"
else fail "API log has no corr=$CORR"; fi

# 7
body="$(http "$BASE_URL/actuator/health" || true)"
if echo "$body" | grep -q '"status"'; then fail "/actuator/health is reachable from outside"; else pass "/actuator is not routed from outside"; fi

# 8 (evidence only; needs the metrics API, which k3s includes)
echo "--- resource use"; kubectl --namespace "$K8S_NAMESPACE" top pods 2>&1 || echo "(metrics not available)"

if [ "$fails" -gt 0 ]; then echo "$fails check(s) failed" >&2; summary "**$fails check(s) failed**"; exit 1; fi
echo "Smoke test passed"; summary "**Smoke test passed**"
