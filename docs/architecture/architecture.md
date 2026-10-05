# Northstar CRM Architecture
Northstar CRM is a slice of a customer management platform for a bank's service agents: an agent searches a customer, reads the profile, and records an interaction.

## Stack

| Layer | Choice |
| --- | --- |
| Frontend | Angular 21, Node 22 LTS, TypeScript |
| API | Spring Boot 4.1.1 on Java 21 |
| Persistence | PostgreSQL 16, Spring Data JPA, Flyway |
| Messaging | Apache Kafka 3.9 |
| Runtime | OpenShift |
| CI/CD | GitHub Actions |
| Infrastructure | Terraform, Ansible |

## Non-goals

- Bitbucket Pipelines 
- Oracle 
- React 
- SOAP 
- k3s 

## Fixtures

| ID | Meaning |
| --- | --- |
| `CUS-1001` | Amina Khan, amina.khan@example.com, ACTIVE |
| `CUS-1002` | Ravi Singh, ravi.singh@example.com, PROSPECT |
| `CUS-9999` | None existent customer |
| `lab-request-001` | Correlation ID |

## Runtime path

```mermaid
flowchart LR
  Agent(["Agent"]) --> UI["Angular"]
  UI --> |"REST + JWT"| API["Spring Boot"]
  API --> |"persists to"| PG[("PostgreSQL")]
  PG -.-> |"transaction commits"| API
  API --> |"after commit, publishes to "| K["Kafka"]
  K --> |"CustomerInteractionRecordedV1"| T[["Topic<br/>crm.customer.interactions.v1"]]
  T --> CON["Consumers"]
  CON -.->|"after bounded retries"| DLT[["DLT<br/>crm.customer.interactions.v1.dlt"]]
```

## Delivery path

```mermaid
flowchart LR
  Dev["Pull request"] --> CI["GitHub Actions CI (security/test/build)"]
  CI --> Main["Merge to main"]
  Main --> |"package once, deploy to"| OS["OpenShift"]
```