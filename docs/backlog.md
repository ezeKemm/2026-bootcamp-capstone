# Backlog

Prioritized, acceptance-backed backlog for the Northstar CRM slice. **CAP-12** is the core feature; the other CAP stories are the related stories CAP-12 and our application depends on.

Acceptance criteria are written as **given / when / then** with the fixtures. The **Evidence** line names the test or file that proves each criterion.

## Fixtures

| Fixture | Value |
|---|---|
| `CUS-1001` | Amina Khan, ACTIVE (`5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11`) |
| `CUS-1002` | Ravi Singh, PROSPECT (`7d3b8c73-99f7-4d06-8ce5-f8d8668ed2a1`) |
| `CUS-9999` | Unknown customer (any UUID not in the database) |
| Correlation ID | `lab-request-001` (sent as `X-Correlation-Id`) |
| Users | `agent1` (AGENT), `admin1` (ADMIN); guest = no token |

`CUS-xxxx` is a label only; the API uses the UUIDs (seeded by Flyway `V2__seed_demo_customers.sql`).

## Priority order and status

| Priority | Story | Status |
|---|---|---|
| 1 | [CAP-12 Record customer interaction](#cap-12--record-customer-interaction) | ✅ Done |
| 2 | [JWT login shell](#jwt-login-shell) | ✅ Done |
| 3 | [Flyway schema and seed data](#flyway-schema-and-seed-data) | ✅ Done |
| 4 | [Guest browses customers](#guest-browses-customers) | ✅ Done |
| 5 | [Customer list and profile](#customer-list-and-profile) | ✅ Done |
| 6 | [Interaction timeline by role](#interaction-timeline-by-role) | ✅ Done |
| 7 | [Activate a prospect](#activate-a-prospect) | ⬜ Not started (backend) |
| 8 | [Actions PR gate](#actions-pr-gate) | ✅ Done |
| 9 | [Audit events cannot be deleted](#audit-events-cannot-be-deleted) | 🟡 Partial |
| 10 | [Deployed smoke test](#deployed-smoke-test) | ⬜ Not started |

## Summarized
### Guest
- [ ] As a guest, I want to list customers so that I can browse customer records.
- [ ] As a guest, I must not be able to record an interaction.
- [ ] As a guest, I must not be able to read a customer's timeline.

### Agent
- [ ] As an agent, I want to read Amina Khan's customer record (CUS-1001, Active).
- [ ] As an agent, I want to read Ravi Singh's customer record (CUS-1002, Prospect).
- [ ] As an agent, I want to record an interaction for an active customer.
- [ ] As an agent, I want to list a customer's interactions.
- [ ] As an agent, I should see only my own write attribution.
- [ ] As an agent, I want to move Ravi Singh from Prospect to Active.

### Admin
- [ ] As an admin, I want to read every customer's timeline.
- [ ] As an admin, I want to move Ravi Singh from Prospect to Active.
- [ ] As an admin, I must not be able to delete an audit event after it is published.

---

## CAP-12 — Record customer interaction

**As** an agent **I want** to record an interaction for `CUS-1001` **so that** the note persists in PostgreSQL and emits a versioned Kafka event.

- [x] **Given** ACTIVE `CUS-1001` and an `agent1` token, **when** I `POST /api/v1/customers/{id}/interactions` with `{ channel, summary }` and `X-Correlation-Id`, **then** I get `201` and the response carries the same correlation ID.
- [x] **Then** the interaction row is in PostgreSQL.
- [x] **Then** a `CustomerInteractionRecordedV1` event with a correlation ID is published to `crm.customer.interactions.v1`.
- [x] **Given** `CUS-9999`, **when** I record, **then** `404` Problem Details.
- [x] **Given** PROSPECT `CUS-1002`, **when** I record, **then** `422` and nothing is saved.
- [x] **Given** a blank summary or unknown channel, **when** I record, **then** `400` with field errors.
- [x] **Given** no token, **when** I record, **then** `401` and nothing is saved.
- [x] **Given** I am signed in as `agent1` in the browser, **when** I record a PHONE interaction on Amina's profile, **then** it shows in her timeline and is still there after reopening the profile.

**Evidence:** `RecordCustomerInteractionIT` (201 + row, 404, 422, 400, 401), `InteractionEventPublisherIntegrationTest`, `CorrelationFilterTest`, `RecordCustomerInteractionControllerTest`, e2e `RecordInteractionTest`.

## Guest browses customers

**As** a guest **I want** to list customers **so that** I can browse customer records without an account.

- [x] **Given** no token, **when** I `GET /api/v1/public/customers`, **then** `200` with names and statuses only (no IDs or emails), paged and filterable by status.
- [x] **Given** no token, **when** I try to record an interaction, **then** `401`.
- [x] **Given** no token, **when** I try to read a timeline, **then** `401`.
- [x] **Given** I open the home page as a guest, **then** I see Amina and Ravi and a **Sign in** button that takes me to the login page.

**Evidence:** `BrowsePublicCustomersControllerTest`, `SecurityPathTest` (`guestCannotRecordAnInteraction`), `ShowCustomerTimelineIT` (`noToken_returns401`), `home.spec.ts`, e2e `GuestHomePageTest`.

## Customer list and profile

**As** an agent or admin **I want** to list customers and open one **so that** I can read `CUS-1001` (ACTIVE) and `CUS-1002` (PROSPECT).

- [x] **Given** a signed-in user, **when** I `GET /api/v1/customers`, **then** `200` with IDs, names, emails and statuses (paged, filterable).
- [x] **Given** `CUS-1001`, **when** I `GET /api/v1/customers/{id}`, **then** `200` with Amina's record; **given** `CUS-9999`, **then** `404`; **given** a non-UUID ID, **then** `400`.
- [x] **Given** I am signed in, **when** I click a customer on `/customers`, **then** their profile opens.

**Evidence:** `BrowseCustomersControllerTest`, `ViewCustomerControllerTest`, `ViewCustomerIT`, `browseCustomers.spec.ts`, `customer-profile.spec.ts`, e2e `RecordInteractionTest` (step 2).

## Interaction timeline by role

**As** an admin **I want** to read every customer's full timeline.
**As** an agent **I want** to see only the interactions I recorded.

- [x] **Given** `admin1`, **when** I `GET /api/v1/customers/{id}/interactions`, **then** I see every interaction, newest first.
- [x] **Given** `agent1`, **then** I see only interactions where I am the actor (actor = JWT username).
- [x] **Given** a customer with no interactions, **then** `200` with an empty list; **given** `CUS-9999`, **then** `404`.

**Evidence:** `ShowCustomerTimelineIT`, `ShowCustomerTimelineControllerTest`, `customer-profile.spec.ts`.

## Activate a prospect

**As** an agent or admin **I want** to move Ravi Singh (`CUS-1002`) from PROSPECT to ACTIVE **so that** interactions can be recorded for him.

- [ ] **Given** PROSPECT `CUS-1002` and an agent or admin token, **when** I `POST /api/v1/customers/{id}/activate`, **then** `200` with status `ACTIVE`.
- [ ] **Given** an already ACTIVE customer, **then** an error response (`422` vs the contract's `409` is still **open**; see `openapi.yaml`).
- [ ] **Given** `CUS-9999`, **then** `404`.
- [ ] **Given** I am signed in, **when** I click **Activate customer** on Ravi's profile, **then** his status shows ACTIVE and the record form appears.

**Status:** the domain rule (`Customer.activate()`), security rule and frontend button exist; the **backend endpoint does not**. The security rule is covered by `SecurityPathTest` (`adminCanActivateACustomer`).

## Audit events cannot be deleted

**As** an admin **I must not** be able to delete an interaction after it is recorded.

- [x] **Given** the API, **then** there is no endpoint that deletes an interaction.
- [ ] **Given** `admin1`, **when** I send `DELETE` to an interaction URL, **then** the request is rejected (needs a test to prove it).
## Actions PR gate

**As** the team **I want** every pull request checked automatically **so that** broken code cannot be merged.

- [x] **Given** a pull request, **then** CI runs backend `mvnw verify` (unit + integration tests), SpotBugs SAST with an SBOM, and frontend lint, tests and production build.
- [x] **Given** a PR title not in `type(scope): summary` form, **then** the title check fails.

**Evidence:** `.github/workflows/ci.yaml`, `pr-title.yaml`, `actionlint.yaml`, PR check history on GitHub.

## JWT login shell

**As** an agent or admin **I want** to sign in **so that** the API knows my role.

- [x] **Given** `agent1`/`agent1`, **when** I `POST /api/v1/auth/login`, **then** `200` with a JWT, my username and role `AGENT` (and `ADMIN` for `admin1`).
- [x] **Given** a wrong password or unknown user, **then** `401` with the same message for both.
- [x] **Given** I am a guest, **when** I open a protected page in the browser, **then** I am sent to the login page and returned to that page after signing in.

**Evidence:** `SecurityPathTest`, `login.spec.ts`, `auth.guard.spec.ts`, `auth.interceptor.spec.ts`, `unauthorized.interceptor.spec.ts`, e2e `RecordInteractionTest`.

## Flyway schema and seed data

**As** the team **I want** the database schema in versioned migrations **so that** every environment starts from the same tables and fixtures.

- [x] **Given** an empty PostgreSQL, **when** the backend starts, **then** Flyway applies `V1` (tables), `V2` (Amina and Ravi) and `V3` (processed events), and Hibernate validates the schema instead of generating it.
- [x] **Given** the integration tests, **when** they run, **then** they use a Testcontainers PostgreSQL built from the same migrations.

**Evidence:** `backend/src/main/resources/db/migration/`, `TestcontainersConfiguration`, all `*IT` tests.

## Deployed smoke test

**As** the team **I want** a smoke test against the deployed app **so that** we know a release works outside our laptops.

- [ ] **Given** a deployed build, **when** the smoke test runs, **then** login and `GET /api/v1/public/customers` succeed.

**Status:** not started. The deploy target is still being decided, and there are no Dockerfiles or deploy workflow yet.
