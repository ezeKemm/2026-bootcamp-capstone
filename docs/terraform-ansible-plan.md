# Terraform and Ansible plan


## Resource Ownership

| Resource | Owner | Notes |
| --- | --- | --- |
| OpenShift Projects (ex. `crm-dev`) | Terraform | Names from [`environment-strategy.md`](environment-strategy.md) |
| ResourceQuota and LimitRange per Project | Terraform | Keeps one environment from using the whole cluster |
| Role bindings for the deploy service account | Terraform | Least privilege: edit rights in its own Project only, never cluster-admin |
| ConfigMap values per environment | Ansible | Rendered from a variables file per environment |
| Secret objects | Created from CI/environment secrets or Ansible Vault at run time | Values never committed |
| PostgreSQL role and database for the CRM, | Ansible | Declarative module, not shell |

If both tools can manage a resource, pick one and write it here.

## Terraform

| Item | Plan |
| --- | --- |
| Scope | `crm-dev`, `crm-test`, `crm-stage` |
| Folder | `infra/terraform/` |
| Providers | Kubernetes provider (OpenShift resources). Versions pinned in `.terraform.lock.hcl` |
| State backend | A remote backend with state locking; never committed. Credentials come from the pipeline's environment secrets and are not written here |
| Variables | `*.tfvars` with secrets are not committed; example variables go in `terraform.tfvars.example` |

## Ansible

| Item | Plan |
| --- | --- |
| Folder | `infra/ansible/` with `inventory/`, `playbooks/`, `roles/`, `group_vars/` |
| Playbooks | `configure-app.yml` (ConfigMap values, environment variables), `database-role.yml` (CRM database role) |
| Checks | `ansible-playbook --syntax-check` and `--check --diff` in CI |
| Secrets | Ansible Vault or CI secrets. The vault password is a CI secret |

### Idempotence

A playbook run a second time with the same inputs changes nothing.

- Safe: a module that states the desired result, such as creating the CRM database role with a declarative module. The second run reports `ok`, not `changed`.
- Not safe: a task that appends a line to a file on every run, or a `shell` command with no guard. The file grows on every run.
- Rule: no `shell` or `command` task without `creates:`, `changed_when:` or `when:` that makes it safe to repeat.

## Forbidden

- A publicly reachable database, or a Route to PostgreSQL or Kafka
- A committed `*.tfstate`, `*.tfstate.backup`, `.terraform/`, or `*.tfvars` holding secrets
- Credentials, tokens or kubeconfigs in HCL, YAML, variables files or comments
- `terraform apply -auto-approve` anywhere except a pipeline job behind an environment approval
- Role bindings broader than the Project they serve, and wildcard permissions
- Using an AI-drafted file without running `validate` and reading the plan ([`ai-usage-plan.md`](ai-usage-plan.md))