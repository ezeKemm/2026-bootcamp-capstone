#!/usr/bin/env bash
# Writes a kubeconfig for the namespace-scoped "deployer" ServiceAccount without ever printing the token.
# Needs, from the GitHub Environment (names only in docs/capstone-cicd-runbook.md):
#   K3S_SERVER (variable)  https://<elastic ip>:6443
#   K3S_CA_DATA (secret)   base64 of the cluster CA (the k3s API certificate is valid for the Elastic IP, see infra/)
#   K3S_TOKEN (secret)     the deployer token of this environment's namespace
#   K8S_NAMESPACE (variable)
# Usage: write-kubeconfig.sh <output-file>
set -euo pipefail
out="${1:?output file}"
: "${K3S_SERVER:?}" "${K3S_CA_DATA:?}" "${K3S_TOKEN:?}" "${K8S_NAMESPACE:?}"
[[ "$K3S_SERVER" =~ ^https://[A-Za-z0-9.-]+(:[0-9]+)?$ ]] || { echo "K3S_SERVER must look like https://host:6443" >&2; exit 2; }
[[ "$K8S_NAMESPACE" =~ ^[a-z0-9]([-a-z0-9]*[a-z0-9])?$ ]] || { echo "K8S_NAMESPACE is not a valid namespace name" >&2; exit 2; }
umask 077
cat > "$out" <<KUBECONFIG
apiVersion: v1
kind: Config
clusters:
  - name: k3s
    cluster:
      server: ${K3S_SERVER}
      certificate-authority-data: ${K3S_CA_DATA}
users:
  - name: crm-deployer
    user:
      token: ${K3S_TOKEN}
contexts:
  - name: crm
    context:
      cluster: k3s
      user: crm-deployer
      namespace: ${K8S_NAMESPACE}
current-context: crm
KUBECONFIG
