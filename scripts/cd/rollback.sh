#!/usr/bin/env bash
# Returns the two Deployments to their previous revision. The rollback unit is the image digest: the previous revision
# runs the previous digest. Manual alternative: run the CD workflow again with the old digests (see the runbook).
# Needs KUBECONFIG and K8S_NAMESPACE. On a first deploy there is no previous revision; the script says so and succeeds.
set -euo pipefail
: "${K8S_NAMESPACE:?}"
k() { kubectl --namespace "$K8S_NAMESPACE" "$@"; }
summary() { if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then echo "$@" >> "$GITHUB_STEP_SUMMARY"; fi; }
for d in crm-api crm-web; do
  revisions="$(k rollout history "deployment/$d" 2>/dev/null | grep -c -E '^[0-9]+ ' || true)"
  if [ "${revisions:-0}" -lt 2 ]; then
    echo "$d has no previous revision (first deploy); nothing to roll back to"
    summary "Rollback: $d had no previous revision."
    continue
  fi
  echo "Rolling $d back"
  k rollout undo "deployment/$d"
  k rollout status "deployment/$d" --timeout=180s
  summary "Rollback: $d now runs \`$(k get "deployment/$d" -o jsonpath='{.spec.template.spec.containers[0].image}')\`"
done
