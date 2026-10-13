# Evidence Index

Fixtures: `CUS-1001` Amina Khan (`5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11`), `CUS-1002` Ravi Singh, correlation ID uses UUID (`lab-request-001`).

## 1. Release

| | |
|---|---|
| Tag / commit | `v0.1.0` / `33d1bfda5a3878b5ba8bbcb7633dac04b3705700` (#135) |
| Backend image | `…/northstar-crm-backend@sha256:b8ed9e0f195edc47d98cbee9566897d40e03868567e9986278e9e3b59420ed6c` |
| Frontend image | `…/northstar-crm-frontend@sha256:b22d10f86334aa0e3d20d5b305e3532492694e59f33961bca81291e6d899bcb9` |
| Runs | [CI #359](https://github.com/ezeKemm/2026-bootcamp-capstone/actions/runs/38333357830) · [CodeQL #42](https://github.com/ezeKemm/2026-bootcamp-capstone/actions/runs/38333357926) · [Image #10](https://github.com/ezeKemm/2026-bootcamp-capstone/actions/runs/38333591476) · [CD to dev #7](https://github.com/ezeKemm/2026-bootcamp-capstone/actions/runs/38333982523) · [failed CD + rollback #6](https://github.com/ezeKemm/2026-bootcamp-capstone/actions/runs/38329822729/job/115085229150) |

Chain: commit → CI + CodeQL → Image builds and scans both images → tag on the same commit → CD deploys the `passed-33d1bfd` images to dev. Nothing is rebuilt after the image build.

## 2. Claims

| ID | Claim | Evidence |
|---|---|---|
| E-01 | Architecture, trust boundary (JWT/RBAC) and decisions are documented | `docs/architecture/{architecture,application-diagrams,security-diagram}.md`; ADRs 0001–0004 |
| E-02 | Measurable NFRs with proof (7 of 8 met) | `docs/nfrs.md` |
| E-03 | Backlog with acceptance criteria, risks with owners, AI usage plan | `docs/backlog.md`, `docs/risk-register.md`, `docs/ai-usage-plan.md`, `docs/team.md` |
| E-04 | CAP-12: record an interaction persists; failures (422, 404, 400, 401) persist nothing | `RecordCustomerInteractionIT`; CI #359 `backend-verify` |
| E-05 | Versioned event, outbox, retries, dead-letter topic, dedupe | `platform/messaging/*`, `V4__create_outbox_event.sql`; `OutboxRelayIT`, `InteractionEventConsumerFailureIT`, `InteractionEventConsumerIT` |
| E-06 | Errors are Problem Details with correlation ID, no internals | `GlobalExceptionHandlerTest` |
| E-07 | Angular journey uses auth, correlation-ID and 401 interceptors | `frontend/src/app/core/http/*.spec.ts`; CI #359 `frontend-verify` |
| E-08 | Schema is versioned and rebuilds from migrations | `db/migration/V1`–`V4`; every `*IT` starts on an empty Testcontainers PostgreSQL |
| E-09 | UI action reaches the database | Selenium `e2e/RecordInteractionTest` (local only, `./mvnw test -Pe2e`) |
| E-10 | No token gives 401; wrong role gives 403 | `SecurityPathTest`; `MetricsIT` (`metricsEndpoint_isForbiddenForAgents`) |
| E-11 | SAST on every PR and push | CI #359 `backend-sast` (SpotBugs + Find Security Bugs, SBOM); CodeQL #42 (Java, TypeScript) |
| E-12 | Images scanned before deploy | Image #10, Trivy 0.70.0 (HIGH/CRITICAL, fixable): backend 0 findings after 3 expiring exceptions for `usr/bin/pebble` in `.trivyignore.yaml` (CVE-2026-78667, -78669, -97031; expire 2026-10-26); frontend 0 findings |
| E-13 | Build and test gates for Java and Angular; coverage gate | `.github/workflows/ci.yaml`; JaCoCo 80% on domain and services (`backend/pom.xml`) |
| E-14 | Images built once, deployed by digest, never rebuilt | `image.yaml`, `cd.yaml`, `scripts/cd/*`; Image #10, CD #7 |
| E-15 | Deployed system works: 9 of 9 smoke checks pass on dev | CD #7 step "Smoke test"; `scripts/cd/smoke-test.sh` |
| E-16 | A bad release is detected and rolled back automatically, then fixed by code | CD #6 on `fb04eec` (3 smoke checks failed, `rollback.sh` restored `crm-api` and `crm-web`) → fix `33d1bfd` → CD #7 |
| E-17 | Infrastructure and manifests are code | `infra/terraform/`, `k8s/`, `infra.yaml` (validate only); ADR 0003 |
| E-18 | Correlation ID, health and metrics are observable | `CorrelationFilterTest`, `LogPatternIT`, `ActuatorIT`, `MetricsIT`; `docs/logging.md`; probes in `k8s/base/crm-api.yaml` |

## 3. Not claimed

| # | Gap | Owner | Date |
|---|---|---|---|
| L-01 | Dependency scan disabled (`RUN_DEP_SCAN` unset); frontend npm covered by Dependabot only | Technical Lead | 2026-10-14 |
| L-02 | NFR-6 (p95 < 500 ms) unmeasured; `crm-api` showed ~1.7 CPU cores after rollout | Technical Lead | 2026-10-14 |
| L-03 | Selenium runs locally only, not in CI or on the deployed URL | Documentation Lead | 2026-10-14 |
| L-04 | Trivy exceptions expire 2026-10-26; "pebble unused at runtime" unverified | Technical Lead | 2026-10-26 |
| L-05 | In-memory demo users (accepted risk); no customer create/update/delete | Technical Lead | accepted |
| L-06 | Trivy secret scan "not scanned"; Terraform applied from a member's machine, not CI | Technical Lead | 2026-10-14 |