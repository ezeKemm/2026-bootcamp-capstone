# Logging Contract

How the backend logs, what every line must carry, and what must never appear in a log. Code that logs follows this contract; `CustomerLoggingIT` and `LogPatternIT` enforce the key parts.

## Format

Console only, one line per event, configured in [`backend/src/main/resources/logback-spring.xml`](../backend/src/main/resources/logback-spring.xml):

```
<UTC timestamp> <LEVEL> [<thread>] <logger> corr=<correlation ID> cust=<customer ID> op=<operation> - <message>
```

Example (one recorded interaction):

```
2026-10-11T19:40:12.481Z INFO  [http-nio-8080-exec-3] c.n.c.r.RecordCustomerInteractionService corr=lab-request-001 cust=5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11 op=record_interaction - recorded interaction id=9f0c… customer id=5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11 channel=PHONE
2026-10-11T19:40:12.490Z INFO  [http-nio-8080-exec-3] c.n.crm.platform.logging.CorrelationFilter corr=lab-request-001 cust=5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11 op=record_interaction - HTTP POST /api/v1/customers/5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11/interactions completed status=201 durationMs=41
```

Levels: root and `com.northstar.crm` at `INFO`.

## Request context (MDC)

| Label | MDC key | Set by | Value |
|---|---|---|---|
| `corr` | `correlationId` | `CorrelationFilter`, every request | `X-Correlation-Id` header if it matches `[A-Za-z0-9._-]{1,60}`, otherwise a new UUID. Echoed in the response header and in every Problem Details body |
| `cust` | `customerId` | The slice's service, via `LogContext.customerOperation(...)` | Customer UUID; empty when the request has no single customer (login, lists) |
| `op` | `operation` | Same call as `cust` | `view`, `activate`, `timeline`, `record_interaction` |

**Lifecycle:** `CorrelationFilter` removes `cust` and `op` and restores `corr` in a `finally` block after logging the request-completed line, so nothing leaks into the next request on the same thread. Services set context; they never clear MDC themselves.

## What each layer logs

| Where | Level | Message |
|---|---|---|
| `CorrelationFilter` | INFO | `HTTP <method> <path> completed status=<code> durationMs=<ms>`: one line per request |
| Services | INFO | One line per successful operation, IDs and outcome only (e.g. `activated customer id=… status=ACTIVE`, `timeline customer id=… scope=own count=2`) |
| `GlobalExceptionHandler` | WARN | Business reject with a reason code: `status=404 reason=customer_not_found`, `status=422 reason=business_rule detail="<domain message>"`, `status=400 reason=validation fields=[…]` |
| `GlobalExceptionHandler` | ERROR | `Unhandled exception` with the stack trace. The client gets a generic 500 with the correlation ID, never the details |
| Kafka publisher / consumer | INFO / WARN / ERROR | Event, interaction and correlation IDs, topic, partition and offset; failed publishes and dead-lettered events |

## Allowed and forbidden

**Allowed:** customer, interaction and event IDs; customer status; interaction channel; operation names; reason codes; HTTP method, path and status; durations; counts; Kafka topic, partition and offset; fixed domain-rule messages.

**Forbidden, in messages and in MDC:**

- customer full name, email, phone or address
- interaction summary (note text)
- passwords, JWTs or the `Authorization` header, the JWT secret, database credentials
- request or response bodies, event payloads
- an entity's `toString()` (it may include any of the above)

Submitted values from a failed validation are not logged; only the field names are.

## How it is proven

| Check | Test |
|---|---|
| The pattern is active and lines carry `corr=` | `LogPatternIT` |
| `cust` and `op` are removed after each request | `CorrelationFilterTest` (`removesCustomerAndOperationAddedDuringTheRequest`) |
| Record and view lines carry `corr` / `cust` / `op`; no name, email, summary or token appears anywhere in the output | `CustomerLoggingIT` (`recordAndView_logIdsAndOperation_withoutPii`) |
| A 422 is logged as WARN with `reason=business_rule` and full context | `CustomerLoggingIT` (`businessRuleReject_isLoggedAsWarnWithReason`) |
| Dead-lettered events don't log the payload | `InteractionEventConsumerFailureIntegrationTest` |

## Finding one request

Every response carries `X-Correlation-Id`, and every error body has a `correlationId` field. Search the backend output for it:

```
# Windows PowerShell (backend output saved to a file)
Select-String -Path backend.log -Pattern "corr=lab-request-001"

# macOS / Linux
grep "corr=lab-request-001" backend.log
```

To trace one customer instead, search for `cust=<customer UUID>`.

## Known gaps

- **Kafka consumer threads have no MDC.** Consumer lines print `corr=` empty, but the correlation ID is in the message itself (`correlationId=…`), so a search for the ID still finds them.
- **Console only.** There is no file appender or central log store yet; the deployed environment is expected to collect container stdout.
- **Plain text, not JSON.** Spring Boot can emit JSON with `logging.structured.format.console: ecs` if a log store needs it later.
