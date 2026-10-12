#!/usr/bin/env bash
# Prints the three values CD needs for one namespace, read from the k3s node over SSH.
#   K3S_SERVER   https://<elastic ip>:6443        (a secret or variable; not sensitive on its own)
#   K3S_CA_DATA  base64 of the cluster CA          (secret)
#   K3S_TOKEN    the namespace's deployer token    (secret, sensitive)
# Usage: infra/scripts/get-deploy-credentials.sh <elastic-ip> <private-key> <namespace>
# Put the output into the matching GitHub Environment (Settings, Environments). Never paste it into chat, an issue, or a file in the repo.
set -euo pipefail
host="${1:?elastic ip}"
key="${2:?path to the private key}"
ns="${3:?namespace, for example crm-dev}"
[[ "$ns" =~ ^[a-z0-9]([-a-z0-9]*[a-z0-9])?$ ]] || { echo "invalid namespace: $ns" >&2; exit 2; }

ssh_run() { ssh -i "$key" -o StrictHostKeyChecking=accept-new "ubuntu@$host" "$@"; }

ssh_run 'test -f /var/lib/crm-bootstrap.done' || { echo "The node is not ready yet. Wait a few minutes after apply and retry (tail /var/log/cloud-init-output.log on the node)." >&2; exit 1; }

echo "K3S_SERVER=https://$host:6443"
echo "K3S_CA_DATA=$(ssh_run 'sudo base64 -w0 /var/lib/rancher/k3s/server/tls/server-ca.crt')"
echo "K3S_TOKEN=$(ssh_run "sudo kubectl -n $ns get secret deployer-token -o jsonpath='{.data.token}'" | base64 -d)"
echo "K8S_NAMESPACE=$ns" >&2
