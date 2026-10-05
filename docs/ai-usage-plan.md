# AI Usage Plan

## Overview
### Purpose and Scope
This plan details how AI can be leveraged to assist in building the Northstar CRM slice. It covers approved types of assistance, rules regarding usage, how to verify output, and how to disclose and track usage. This plan applies to all AI tools used (chat assistants, IDE plugins, agentic tools).

### Ownership
The person who commits AI-generated work owns the work and is expected to take responsibility for any generated content.


## Allowed AI-Assistance

| Use | Examples | Caveats |
| --- | --- | --- |
| Design | Brainstorming, planning, discovery and research | Cannot make decisions |
| Documentation | Documentation drafts, proofreading, diagrams, reports/runbook drafts | Drafts only, always requires human review |
| Boilerplate | DTOs, mappers, Angular components, Spring classes | Code generation in small, maintainable snippets | 
| Tests | Test skeletons, Unit tests, Integration Tests, Edge cases | Tests are not weakened or deleted |
| Infrastructure | IaC drafts, OpenShift manifests, workflow YAML | Linted and verified | 
| Explanation | Explaining an error, a stack trace or an unfamiliar API; codebase exploration | Only provide errors with secrets and identifiers removed |
| Assisted Code Review | Automated CI review of PRs | All PRs require human review |

### Must verify

| Output | Check before it is committed | Mechanism |
| --- | --- | --- |
| Angular code | It compiles, matches contracts, every template binding is read by a person | `ng build` and `ng test` pass; service types compared with the contract; no `any` added to silence the compiler |
| Java code | No leakage between layers; no hallucinated annotations, transaction boundaries are respected | `./mvnw verify` |
| Flyway / SQL | Correct PostgreSQL syntax and types; no destructive statement (`DROP`, `ALTER`, `TRUNCATE`) | Migration applied forward to an empty scratch database; `flyway_schema_history` inspected; a person reads every statement |
| Tests | They fail when the behaviour is wrong | Break the code on purpose once and watch the test fail |
| Terraform / Ansible | Scope is non-prod and idempotent; no secrets; no public database | `terraform fmt`, `validate`, and `plan` reviewed; Ansible run twice shows `changed=0` on the second run |
| Workflow YAML | Actions pinned by SHA; minimal `permissions`; inputs not pasted into scripts | `actionlint` clean; CODEOWNERS reviewer on `.github/workflows/` |
| Dependencies | The package exists, is the intended one, and is current | Check the registry page and the lockfile diff |
| Anything with a number or a claim | The number is true | Run it or look it up; do not copy a threshold or a version from a prompt answer |

## Rules

1. **Never** enter Customer PII into a prompt: real names, addresses, account numbers, SSNs, emails, and phone numbers. Use fixtures (`CUS-1001`) only.
2. **Never** use environment files and secrets into a prompt: `.env`, API keys, tokens, passwords, connection strings
3. **Never** give an AI any file marked closed or private (config, credentials, internal deployment files)
4. **Never** let an AI generate sensitive data such as secrets, keys, or passwords to be used in production.
5. **Never** let an agentic tool execute commands against a production environment.
6. **Never** let an agentic tool push to `main`, merge PRs, or approve it's own work. 
7. **Never** commit AI-generated work that copies propietary or third-party code with an incompatible license.


## Documentation and Review of AI Use
1. The author reviews and understands AI-generated code/content before opening a PR. 
2. AI-usage is attributed per file in the PR. The PR names who verified each output and how the output was verified.
3. The technical lead must review and approve all files with AI-generated code.
4. The documentation lead must review and approve all documentation that includes AI-usage.