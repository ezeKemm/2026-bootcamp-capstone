# Non-Functional Requirements

Measurable NFRs for the Northstar CRM slice, covering the five categories the capstone brief requires (§2.4): **security, traceability, recoverability, performance and operability**. Each NFR has a **metric**, a **target** and the **proof** (test, log or command) that shows whether it is met, following Lab 48 Exercise 2 ("metric + target + how Labs 49–52 prove it").

**Status:** ✅ Met (proven by the named evidence) · 🟡 Partial · ⬜ Not yet measured. Gaps are also tracked in the risk register.

## Summary

| ID | Category | Requirement | Status |
|---|---|---|---|
| NFR-1 | Security | Protected endpoints reject requests without a valid token | ✅ Met |
| NFR-2 | Traceability | Every request and event carries a correlation ID | ✅ Met |
| NFR-3 | Recoverability | Failed event processing is retried, then dead-lettered, never lost or duplicated | ✅ Met |
| NFR-4 | Recoverability | A recorded interaction is never lost when Kafka is down | 🟡 Partial |
| NFR-5 | Recoverability | The database can be rebuilt from migrations alone | ✅ Met |
| NFR-6 | Performance | Core API calls respond quickly in the lab environment | ⬜ Not yet measured |
| NFR-7 | Operability | Errors are diagnosable without leaking internals | ✅ Met |
| NFR-8 | Operability | The app can be started and health-checked from documented steps | 🟡 Partial |

## Details

### NFR-1 — Security: deny by default

| | |
|---|---|
| **Metric** | Share of protected endpoints that return `401` without a token or with an invalid one |
| **Target** | 100%. The only endpoints open to guests are `POST /api/v1/auth/login` and `GET /api/v1/public/customers`; AGENT and ADMIN tokens pass the role rule for each protected endpoint |
| **Proof** | `SecurityPathTest` (401 without or with an invalid token; agent can record, admin can activate and read the timeline; guest can browse); `noToken_returns401` in `RecordCustomerInteractionIT` and `ShowCustomerTimelineIT` |
| **Status** | ✅ Met |

### NFR-2 — Traceability: correlation ID end to end

| | |
|---|---|
| **Metric** | Share of HTTP requests whose correlation ID appears in the `X-Correlation-Id` response header, the request log line, any error body, and the Kafka event for that request |
| **Target** | 100%. A valid incoming ID (e.g. `lab-request-001`) is reused; a missing or invalid one is replaced by a generated UUID |
| **Proof** | `CorrelationFilterTest` (reuse, generate, replace invalid); `GlobalExceptionHandlerTest` (correlation ID in Problem Details); `InteractionEventPublisherIntegrationTest` (`lab-request-001` in the published `CustomerInteractionRecordedV1`) |
| **Status** | ✅ Met |

### NFR-3 — Recoverability: event consumer failures

| | |
|---|---|
| **Metric** | Outcome of a failed event: retries attempted, where it ends up, and how often a repeated event is processed |
| **Target** | A transient failure is retried up to **3 attempts, 1 s apart**; after 3 failures, or for malformed JSON or an unsupported event version, the event goes to `crm.customer.interactions.v1.dlt` with its original payload; a duplicate event is processed **exactly once** |
| **Proof** | `InteractionEventConsumerFailureIntegrationTest` (succeeds on 3rd attempt; dead-letter after 3; malformed JSON and unsupported version go to the DLT); `InteractionEventConsumerIntegrationTest` (`handlesARepeatedEventOnlyOnce`) |
| **Status** | ✅ Met |

### NFR-4 — Recoverability: Kafka outage while recording

| | |
|---|---|
| **Metric** | (a) Whether `POST …/interactions` still saves the interaction when Kafka is down; (b) time before the failed publish is reported; (c) whether the missed event is sent later |
| **Target** | (a) Saved, `201`; (b) publish fails within **10 s** (`delivery.timeout.ms`) and is logged with the interaction and correlation IDs; (c) the event is eventually published |
| **Proof** | (a)(b) The event is published only after the database commit (`InteractionRecordedListener`, `@TransactionalEventListener(AFTER_COMMIT)`); `InteractionRecordedListenerTest` (`failedPublishIsSwallowed`); producer timeouts in `application.yaml` |
| **Status** | 🟡 Partial: (a) and (b) are met, (c) is not. There is no outbox or re-publish job, so an event missed during an outage is only in the logs. Listed as a residual risk |

### NFR-5 — Recoverability: rebuild the database from scratch

| | |
|---|---|
| **Metric** | Steps needed to get an empty PostgreSQL to the working schema and demo data |
| **Target** | One application start. Flyway applies `V1`–`V3`; Hibernate only validates (`ddl-auto: validate`), so no hand-made changes are needed |
| **Proof** | Every `*IT` test starts from an empty Testcontainers PostgreSQL and passes; `docker compose down -v` followed by a backend start restores Amina and Ravi (README) |
| **Status** | ✅ Met |

### NFR-6 — Performance: API response time

| | |
|---|---|
| **Metric** | p95 server-side response time of `GET /api/v1/customers/{id}` and `POST /api/v1/customers/{id}/interactions` |
| **Target** | **p95 < 500 ms** on the local lab setup (Docker PostgreSQL and Kafka), over 20 calls each |
| **Proof** | Every request is logged by `CorrelationFilter` as `HTTP <method> <path> completed status=… durationMs=…`. Method: make 20 calls, sort the `durationMs` values, read the 19th |
| **Status** | ⬜ Not yet measured. Record the result here before the defense |

### NFR-7 — Operability: diagnosable errors

| | |
|---|---|
| **Metric** | Share of error responses that are `application/problem+json` with `title`, `status` and `correlationId`, and share of unexpected errors that leak internal details |
| **Target** | 100% Problem Details (400 with per-field errors, 401, 403, 404, 422, 500); **0** stack traces or exception messages in 500 responses (they go to the log instead) |
| **Proof** | `GlobalExceptionHandlerTest` (404, 422, 400 with field errors, 500 hides internals, unknown URL stays 404); `SecurityPathTest` (401 Problem Details); `RecordCustomerInteractionIT` (400/404/422) |
| **Status** | ✅ Met |

### NFR-8 — Operability: start and health-check

| | |
|---|---|
| **Metric** | (a) Commands needed to run the full app locally from the README; (b) a health endpoint that reports up/down |
| **Target** | (a) One command for dependencies (`docker compose up -d`) plus one each for backend and frontend; (b) a health endpoint returning `UP` when the database and Kafka are reachable |
| **Proof** | (a) README "Run it locally", followed on a clean checkout; (b) none yet: the backend has no health endpoint (Spring Boot Actuator is not included) |
| **Status** | 🟡 Partial: (a) met, (b) not built. Listed as a gap for the release lab |