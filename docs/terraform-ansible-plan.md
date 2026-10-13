# Terraform and Ansible plan

Infrastructure for the Northstar CRM runs on AWS. Terraform creates one single-node k3s cluster per deployed environment. CD deploys the application onto it. The target is decided in [ADR 0003](architecture/adrs/0003-k3s.md).

**Ansible is not used.** Configuration is applied by CD from the manifests in `k8s/` (Kustomize) and the scripts in `scripts/cd/`. This file keeps its name so existing links work.

## Resource ownership

| Resource | Owner | Notes |
| --- | --- | --- |
| EC2 instance per environment (`dev`, `stage`), Elastic IP, security group, key pair | Terraform | `infra/terraform/main.tf`. Default size `t3.large`, region `us-west-2` |
| Encrypted root volume (30 GB gp3) and IMDSv2-only metadata | Terraform | PostgreSQL and Kafka data live on this volume (k3s `local-path` storage) |
| k3s install (pinned version) with Traefik ingress | cloud-init, rendered by Terraform | `infra/terraform/cloud-init.yaml.tftpl`. Runs once at first boot |
| Namespaces (`crm-dev`, `crm-stage`) | cloud-init bootstrap script | One namespace per instance |
| `deployer` ServiceAccount, Role, RoleBinding and token Secret | cloud-init bootstrap script | Least privilege: the Role covers only the application's own kinds in its own namespace, never cluster-admin |
| Deployments, Services, Ingress, StatefulSets, ConfigMaps | CD | `k8s/base` plus `k8s/overlays/dev` and `k8s/overlays/stage`, applied by `scripts/cd/deploy.sh` |
| Secret objects (database password, JWT secret, demo passwords, registry pull token) | CD, from GitHub Environment secrets | Written at deploy time from a private temporary file. Values are never committed or printed |
| PostgreSQL user and database for the CRM | CD manifests | `k8s/base/postgres.yaml` and the Secret written by `deploy.sh` |

One owner per resource. If two tools could manage the same resource, pick one and write it here.

## Terraform

| Item | Plan |
| --- | --- |
| Scope | Two instances: `dev` (namespace `crm-dev`) and `stage` (namespace `crm-stage`), each a separate single-node cluster with its own Elastic IP |
| Folder | `infra/terraform/` |
| Providers | `hashicorp/aws` `~> 6.0`, Terraform `>= 1.6`. Versions pinned in `.terraform.lock.hcl` (committed) |
| State | Local state on the machine that runs `apply`. It is git-ignored and never committed. A remote backend with locking is not set up (see Known gaps) |
| Variables | `terraform.tfvars` is git-ignored. Example values go in `terraform.tfvars.example`. Only the SSH public key is passed in; the private key never enters Terraform |
| Checks in CI | `.github/workflows/infra.yaml` runs `terraform fmt -check`, `terraform init -backend=false` and `terraform validate`. It uses no AWS credentials |
| Plan and apply | Run from a team member's machine after reading the plan. No pipeline job applies infrastructure |
| Outputs | Per instance: public IP, application URL, Kubernetes API URL, namespaces, SSH command |
| CD credentials | `infra/scripts/get-deploy-credentials.sh` prints `K3S_SERVER`, `K3S_CA_DATA` and `K3S_TOKEN` for one namespace. They go into the matching GitHub Environment and nowhere else |

### Network and access

| Port | Default | Purpose |
| --- | --- | --- |
| 80, 443 | Open to `0.0.0.0/0` (`web_allowed_cidrs`) | Application. Only HTTP is served today |
| 6443 | Open to `0.0.0.0/0` (`api_allowed_cidrs`) | Kubernetes API for CD. GitHub-hosted runners have no fixed addresses. Protected by TLS and a namespace-scoped token |
| 22 | Closed (`ssh_allowed_cidrs` is empty) | SSH for the team. Open it to named `/32` addresses only |

The Ingress routes `/api` to the API and `/` to the web app. `/actuator` is not routed, so health and metrics stay inside the cluster.

### Idempotence

A second `terraform apply` with the same inputs changes nothing.

- Safe: resources that state the desired result, and the bootstrap script, which uses `kubectl apply` for every object.
- Not safe: a task that appends a line to a file on every run, or a `shell` command with no guard. The file grows on every run.
- Rule: no script step without a guard (`--dry-run=client | apply`, `creates:`, or an existence check) that makes it safe to repeat.
- Changing the cloud-init template does not replace a running instance (`user_data_replace_on_change = false`). A newer Ubuntu image does not either (`ignore_changes = [ami]`). Replace an instance deliberately.

## Forbidden

- A publicly reachable database, or an Ingress route or open port to PostgreSQL or Kafka
- A committed `*.tfstate`, `*.tfstate.backup`, `.terraform/`, `terraform.tfvars` or private key
- Credentials, tokens or kubeconfigs in HCL, YAML, variables files, scripts or comments
- `terraform apply -auto-approve`
- Role bindings broader than the namespace they serve, and wildcard permissions
- Using an AI-drafted file without running `fmt` and `validate` and reading the plan ([`ai-usage-plan.md`](ai-usage-plan.md))

## Known gaps

Accepted for the capstone. Track them in the [risk register](risk-register.md).

- **No remote state or locking.** State lives on one machine. Losing it means importing resources by hand.
- **Kubernetes API open to the Internet by default.** TLS and a namespace-scoped token limit the risk. Restrict `api_allowed_cidrs` if CD gets fixed addresses.
- **No TLS.** The application is served over HTTP.
- **Single node per environment.** No high availability. Data sits on the root volume with no backup.
- **No ResourceQuota or LimitRange.** One workload can use a whole node.
- **No `test` or `prod` environment.** Only `dev` and `stage` exist.
- **No NetworkPolicy by default.** `k8s/optional/networkpolicy.yaml` limits PostgreSQL and Kafka to the API, but it is not applied automatically.