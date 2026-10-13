# Northstar CRM (2026 bootcamp capstone)

A vertical slice of Northstar Bank's CRM: service agents browse customers, open a customer's profile, activate prospects and record interactions (phone, email, chat, branch). Each recorded interaction is saved to PostgreSQL and published to Kafka as an event through a transactional outbox.

**Stack:** Java 21 · Spring Boot 4.1 · Spring Security (JWT) · Spring Data JPA + Flyway · PostgreSQL · Kafka · Angular 22 · GitHub Actions · Docker · k3s on AWS (Terraform)

## Repository layout

| Path | What it is |
|---|---|
| `backend/` | Spring Boot API. Vertical slices under `com.northstar.crm` (`browsePublicCustomers`, `browseCustomers`, `viewCustomer`, `activateCustomer`, `recordCustomerInteraction`, `showCustomerTimeline`), login in `auth/`, shared model in `domain/`, cross-cutting code in `platform/` (security, logging, exceptions, messaging, actuator). |
| `backend/src/main/resources/openapi.yaml` | API contract (draft). |
| `backend/src/main/resources/db/migration/` | Flyway migrations: tables, demo seed data, processed events, outbox. |
| `frontend/` | Angular app (login, customer list, customer profile, activate button, record-interaction form, timeline). |
| `docs/` | Architecture, ADRs, backlog, NFRs, risk register, team agreements and plans (see [Documentation](#documentation)). |
| `compose.yaml` | Local PostgreSQL and Kafka. The `full` profile also builds and runs the backend and frontend images. |
| `docker/` | Local nginx config that proxies `/api` for the `full` profile. |
| `k8s/` | Kustomize manifests: `base/` plus `overlays/dev` and `overlays/stage`. `optional/networkpolicy.yaml` is not applied by default. |
| `infra/` | Terraform for the AWS EC2 hosts that run k3s (one per environment), cloud-init bootstrap, and `scripts/get-deploy-credentials.sh`. |
| `scripts/cd/` | Deploy, smoke test, rollback and digest scripts used by the CD workflow. |
| `.github/workflows/` | CI, image build and scan, CD, CodeQL, dependency scan, Terraform validation, PR title check, workflow lint (see [CI and deployment](#ci-and-deployment)). |

## Run it locally

**Prerequisites:** JDK 21, Node 22 + npm, Docker Desktop (running), Chrome (only for e2e tests).

1. **Configure:** copy `.env.example` to `.env` in the repo root and fill in `DB_PASSWORD` and `JWT_SECRET` (at least 32 random characters). `.env` is git-ignored; never commit it.
2. **Start PostgreSQL and Kafka** (repo root):
   ```
   docker compose up -d
   ```
3. **Start the backend** (`backend/`), on http://localhost:8080. Flyway creates the tables and seeds the demo customers on first start.
   ```
   ./mvnw spring-boot:run        # Windows: .\mvnw spring-boot:run
   ```
4. **Start the frontend** (`frontend/`), on http://localhost:4200:
   ```
   npm install
   npm start
   ```

Stop everything with Ctrl+C in the backend and frontend terminals, then `docker compose down` (add `-v` to also wipe the database).

**Alternative: everything in containers.** Skip steps 3 and 4 and run `docker compose --profile full up -d --build` from the repo root. The app is on http://localhost:4200 (change the port with `WEB_PORT` in `.env`).

### Demo data

| Login | Password | Role |
|---|---|---|
| `agent1` | `agent1` | AGENT |
| `admin1` | `admin1` | ADMIN |

Lab-only demo accounts, overridable with `CRM_AGENT_PASSWORD` / `CRM_ADMIN_PASSWORD`.

Seeded customers: **Amina Khan** (ACTIVE, `5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11`) and **Ravi Singh** (PROSPECT, `7d3b8c73-99f7-4d06-8ce5-f8d8668ed2a1`).

Without logging in, the home page shows a guest view (names and statuses only, from `GET /api/v1/public/customers`). After logging in, `/customers` lists customers with links to their profiles.

### Health and metrics

`GET /actuator/health/liveness` and `/actuator/health/readiness` are public and show no details; the smoke test and container health checks use them. `/actuator/metrics/**` is ADMIN only.

## Run the tests

| What | Where | Command |
|---|---|---|
| Backend unit and controller tests | `backend/` | `./mvnw test` |
| Backend full build as in CI: adds integration tests (`*IT`, Testcontainers, needs Docker) and the JaCoCo coverage gate (80% on services and domain) | `backend/` | `./mvnw verify` |
| Backend SAST (SpotBugs + Find Security Bugs) | `backend/` | `./mvnw -Psecurity-scan -DskipTests verify` |
| Frontend unit tests (Vitest) | `frontend/` | `npm test -- --watch=false` |
| Frontend lint (local only, not run in CI) | `frontend/` | `npm run lint` |
| End-to-end browser tests (Selenium) | `backend/` | `./mvnw test -Pe2e` |

End-to-end tests are tagged `e2e` and **skipped by a normal `mvnw test` and in CI**. They need Chrome and the whole app running (steps 2–4 above). They write real interactions to the local database.

## CI and deployment

- **CI** (`ci.yaml`): backend `verify`, SpotBugs SAST, a packaged jar with checksums on `main`, and the frontend production build plus tests. CodeQL, a dependency scan, a PR title check and workflow lint run as separate workflows.
- **Image** (`image.yaml`): after CI passes on `main`, builds the backend and frontend images, pushes them to GHCR, scans each by digest with Trivy (a fixable HIGH or CRITICAL finding fails the job), and tags passing images `passed-<commit>`.
- **CD** (`cd.yaml`): started by hand or by a `v*` tag. Deploys the two image digests to the `dev` or `stage` GitHub Environment, runs a smoke test and rolls back on failure. Secrets come from the GitHub Environment and are never committed.
- **Infrastructure** (`infra/terraform/`): one single-node k3s host per environment on AWS, created with Terraform. `infra.yaml` only checks formatting and validity; a team member runs plan and apply by hand. See [ADR 0003](docs/architecture/adrs/0003-k3s.md) and the [Terraform plan](docs/terraform-ansible-plan.md).

## Documentation

- [Architecture](docs/architecture/architecture.md), [application diagrams](docs/architecture/application-diagrams.md), [security diagram](docs/architecture/security-diagram.md)
- [Architecture decision records](docs/architecture/adrs/)
- [API contract](backend/src/main/resources/openapi.yaml)
- [Product backlog](docs/backlog.md), [non-functional requirements](docs/nfrs.md), [risk register](docs/risk-register.md)
- [Team roles and working agreements](docs/team.md)
- [AI usage plan](docs/ai-usage-plan.md)
- [Logging](docs/logging.md)
- Plans: [GitHub Actions](docs/github-actions-plan.md), [environments](docs/environment-strategy.md), [Terraform](docs/terraform-ansible-plan.md)

## Current status

Working: guest browsing, JWT login with AGENT/ADMIN roles, customer list and profile, activating a prospect, recording interactions (saved with an outbox row in one transaction, published to Kafka by a relay, with consumer retries, deduplication and a dead-letter topic), Problem Details errors with correlation IDs, health and metrics endpoints, CI with tests and SAST, image build and scan, and deployment to k3s on AWS with a smoke test and rollback.

Known gaps (tracked in the [risk register](docs/risk-register.md)):
- **No TLS:** the deployed app is served over HTTP.
- **Terraform state is local:** no remote backend or locking.
- **Single node per environment:** no high availability or backup.
- **Tests:** e2e tests run only on a developer machine; there is no expired-token test.
- **Docs:** some documents still describe the earlier OpenShift plan. The risk register lists which.