# Northstar CRM (2026 bootcamp capstone)

A vertical slice of Northstar Bank's CRM: service agents browse customers, open a customer's profile, activate prospects and record interactions (phone, email, chat, branch). Each recorded interaction is saved to PostgreSQL and published to Kafka as an event.

**Stack:** Java 21 · Spring Boot 4.1 · Spring Security (JWT) · Spring Data JPA + Flyway · PostgreSQL · Kafka · Angular 22 · GitHub Actions

## Repository layout

| Path | What it is |
|---|---|
| `backend/` | Spring Boot API. Vertical slices under `com.northstar.crm` (`browsePublicCustomers`, `browseCustomers`, `viewCustomer`, `recordCustomerInteraction`, `showCustomerTimeline`), shared model in `domain/`, cross-cutting code in `platform/` (security, logging, exceptions, messaging). |
| `backend/src/main/resources/openapi.yaml` | API contract (draft). |
| `backend/src/main/resources/db/migration/` | Flyway migrations (tables, demo seed data, processed events). |
| `frontend/` | Angular app (login, customer list, customer profile, record-interaction form). |
| `docs/` | Architecture, ADRs, backlog, team agreements and plans (see [Documentation](#documentation)). |
| `compose.yaml` | Local PostgreSQL and Kafka. |
| `.github/workflows/` | CI: backend tests + SpotBugs SAST + SBOM, frontend lint/test/build, PR title check, workflow lint, dependency scan. |

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

### Demo data

| Login | Password | Role |
|---|---|---|
| `agent1` | `agent1` | AGENT |
| `admin1` | `admin1` | ADMIN |

Lab-only demo accounts, overridable with `CRM_AGENT_PASSWORD` / `CRM_ADMIN_PASSWORD`.

Seeded customers: **Amina Khan** (ACTIVE, `5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11`) and **Ravi Singh** (PROSPECT, `7d3b8c73-99f7-4d06-8ce5-f8d8668ed2a1`).

Without logging in, the home page shows a guest view (names and statuses only, from `GET /api/v1/public/customers`). After logging in, `/customers` lists customers with links to their profiles.

## Run the tests

| What | Where | Command |
|---|---|---|
| Backend unit, controller and integration tests (Testcontainers, needs Docker) | `backend/` | `./mvnw test` |
| Backend full build as in CI | `backend/` | `./mvnw verify` |
| Backend SAST (SpotBugs + Find Security Bugs) | `backend/` | `./mvnw -Psecurity-scan -DskipTests verify` |
| Frontend unit tests (Vitest) | `frontend/` | `npm test -- --watch=false` |
| Frontend lint | `frontend/` | `npm run lint` |
| End-to-end browser tests (Selenium) | `backend/` | `./mvnw test -Pe2e` |

End-to-end tests are tagged `e2e` and **skipped by a normal `mvnw test` and in CI**. They need Chrome and the whole app running (steps 2–4 above). They write real interactions to the local database.

## Documentation

- [Architecture](docs/architecture/architecture.md), [application diagrams](docs/architecture/application-diagrams.md), [security diagram](docs/architecture/security-diagram.md)
- [Architecture decision records](docs/architecture/adrs/)
- [API contract](backend/src/main/resources/openapi.yaml)
- [Product backlog](docs/backlog.md)
- [Team roles and working agreements](docs/team.md)
- [AI usage plan](docs/ai-usage-plan.md)
- Plans: [GitHub Actions](docs/github-actions-plan.md), [environments](docs/environment-strategy.md), [Terraform/Ansible](docs/terraform-ansible-plan.md)

## Current status

Working locally: guest browsing, JWT login with AGENT/ADMIN roles, customer list and profile, recording interactions (saved and published to Kafka, with retries and a dead-letter topic), Problem Details errors with correlation IDs, and CI with tests and SAST.

Not built yet:
- **Activate customer:** the frontend button and security rule exist, but the backend endpoint does not.
- **Deployment:** no Dockerfiles, image workflow or deploy target yet. The plans in `docs/` describe the intended setup.