#!/usr/bin/env bash
# Deploys two image digests to one environment. It never builds anything.
# Order: config, then the data tier (PostgreSQL, Kafka) and wait for it, then the application tier and wait for it.
# The API creates its Kafka topics at start, so Kafka must be up before the API starts; that is why there are tiers.
# Needs KUBECONFIG pointing at the environment (scripts/cd/write-kubeconfig.sh) and, in the environment:
#   ENVIRONMENT, K8S_NAMESPACE, IMAGE_OWNER (lower case), BACKEND_DIGEST, FRONTEND_DIGEST, PUBLIC_ORIGIN
#   secrets: DB_PASSWORD, JWT_SECRET, CRM_AGENT_PASSWORD, CRM_ADMIN_PASSWORD; optional: GHCR_PULL_USER, GHCR_PULL_TOKEN
# Usage (from the repository root): scripts/cd/deploy.sh
set -euo pipefail
set +x # never trace: this script handles secrets

: "${ENVIRONMENT:?}" "${K8S_NAMESPACE:?}" "${IMAGE_OWNER:?}" "${BACKEND_DIGEST:?}" "${FRONTEND_DIGEST:?}" "${PUBLIC_ORIGIN:?}"
: "${DB_PASSWORD:?}" "${JWT_SECRET:?}" "${CRM_AGENT_PASSWORD:?}" "${CRM_ADMIN_PASSWORD:?}"
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
"$here/validate-digest.sh" "$BACKEND_DIGEST" "$FRONTEND_DIGEST"
[[ "$ENVIRONMENT" =~ ^(dev|stage)$ ]] || { echo "unknown environment: $ENVIRONMENT" >&2; exit 2; }
[[ "$IMAGE_OWNER" =~ ^[a-z0-9]([-a-z0-9]*[a-z0-9])?$ ]] || { echo "IMAGE_OWNER must be lower case" >&2; exit 2; }
[[ "$PUBLIC_ORIGIN" =~ ^https?://[A-Za-z0-9.:-]+$ ]] || { echo "PUBLIC_ORIGIN must look like http://<address>" >&2; exit 2; }
[ -n "${JWT_SECRET}" ] && [ "${#JWT_SECRET}" -ge 32 ] || { echo "JWT_SECRET must have at least 32 characters" >&2; exit 2; }

k() { kubectl --namespace "$K8S_NAMESPACE" "$@"; }
summary() { if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then echo "$@" >> "$GITHUB_STEP_SUMMARY"; fi; }

echo "Deploying to $ENVIRONMENT (namespace $K8S_NAMESPACE)"
k auth can-i update deployments >/dev/null || { echo "The deployer token cannot update deployments in $K8S_NAMESPACE" >&2; exit 1; }
prev_api="$(k get deployment crm-api -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)"
prev_web="$(k get deployment crm-web -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)"
summary "Previous API image: \`${prev_api:-none (first deploy)}\`"
summary "Previous web image: \`${prev_web:-none (first deploy)}\`"

# Secrets and environment-specific settings. Written through a private temporary file, never printed.
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
umask 077
{
  echo "DB_USERNAME=crm"
  echo "DB_PASSWORD=$DB_PASSWORD"
  echo "JWT_SECRET=$JWT_SECRET"
  echo "CRM_AGENT_PASSWORD=$CRM_AGENT_PASSWORD"
  echo "CRM_ADMIN_PASSWORD=$CRM_ADMIN_PASSWORD"
} > "$tmp/secrets.env"
k create secret generic crm-secrets --from-env-file="$tmp/secrets.env" --dry-run=client -o yaml | k apply -f - >/dev/null
k create configmap crm-env --from-literal=CORS_ALLOWED_ORIGIN="$PUBLIC_ORIGIN" --dry-run=client -o yaml | k apply -f - >/dev/null
if [ -n "${GHCR_PULL_USER:-}" ] && [ -n "${GHCR_PULL_TOKEN:-}" ]; then
  k create secret docker-registry ghcr-pull --docker-server=ghcr.io --docker-username="$GHCR_PULL_USER" \
    --docker-password="$GHCR_PULL_TOKEN" --dry-run=client -o yaml | k apply -f - >/dev/null
  echo "Image pull secret applied"
else
  echo "No GHCR_PULL_USER/GHCR_PULL_TOKEN: the images must be public"
fi

# A throwaway overlay (git-ignored) that sets the two image digests on top of the environment's overlay.
run="$repo/k8s/overlays/_run"
rm -rf "$run"; mkdir -p "$run"
cat > "$run/kustomization.yaml" <<OVERLAY
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - ../$ENVIRONMENT
images:
  - name: northstar-crm-backend
    newName: ghcr.io/$IMAGE_OWNER/northstar-crm-backend
    digest: $BACKEND_DIGEST
  - name: northstar-crm-frontend
    newName: ghcr.io/$IMAGE_OWNER/northstar-crm-frontend
    digest: $FRONTEND_DIGEST
OVERLAY

echo "1/3 config and data tier"
kubectl apply -k "$run" -l crm.northstar/tier=config
kubectl apply -k "$run" -l crm.northstar/tier=data
k rollout status statefulset/postgres --timeout=300s
k rollout status statefulset/kafka --timeout=300s

echo "2/3 application tier"
kubectl apply -k "$run" -l crm.northstar/tier=app
if [ -n "${GITHUB_OUTPUT:-}" ]; then echo "applied=true" >> "$GITHUB_OUTPUT"; fi # from here on a failure triggers the rollback step

echo "3/3 waiting for the rollout"
k rollout status deployment/crm-api --timeout=300s
k rollout status deployment/crm-web --timeout=180s

now_api="$(k get deployment crm-api -o jsonpath='{.spec.template.spec.containers[0].image}')"
now_web="$(k get deployment crm-web -o jsonpath='{.spec.template.spec.containers[0].image}')"
[ "$now_api" = "ghcr.io/$IMAGE_OWNER/northstar-crm-backend@$BACKEND_DIGEST" ] || { echo "API runs $now_api, expected the requested digest" >&2; exit 1; }
[ "$now_web" = "ghcr.io/$IMAGE_OWNER/northstar-crm-frontend@$FRONTEND_DIGEST" ] || { echo "Web runs $now_web, expected the requested digest" >&2; exit 1; }
summary "Now running: API \`$now_api\`, web \`$now_web\`"
echo "Deployed."
