# Application Diagrams

### Application Flow

#### Record Customer Interaction: Happy Path
```mermaid
sequenceDiagram
    actor User as Agent
    participant Client as Angular
    participant Controller as Spring Boot<br/>Controller
    participant Service as Spring Boot<br/>Service
    participant Domain as Customer
    participant Repo as JPA Repository
    participant DB as PostgreSQL
    participant Notif as Notification Service
    participant Kafka
    autonumber

    User->>Client: Record Interaction
    Client->>Controller: POST /api/v1/customers/CUS-1001/interaction
    Controller->>Controller: Validate request
    Controller->>Service: CustomerInteractionRequest DTO
    Service->>DB: BEGIN transaction
    Service->>Repo: Find CUS-1001
    Repo->>DB: SELECT for CUS-1001
    DB-->>Repo: Return record
    Repo-->>Service: Return Customer
    Service->>Domain: Add interaction
    Service->>Repo: Save interaction
    Repo->>DB: INSERT interaction record
    DB-->>Repo: Confirm insertion
    Repo-->>Service: Return updated Customer
    Service->>DB: COMMIT transaction
    Service->>Notif: Call Notification Service
    Notif->>Kafka: Send CustomerInteractionRecordedV1
    Service-->>Controller: CustomerInteractionResponse DTO
    Controller-->>Client: 201 Created
    Client-->>User: Render Dashboard
```

#### Validation Error Path
```mermaid
sequenceDiagram
    actor User as Agent
    participant Client as Angular
    participant Controller as Spring Boot<br/>Controller
    participant Valid as Hibernate Validator
    autonumber

    User->>Client: Record Interaction
    Client->>Controller: POST /api/v1/customers/CUS-1001/interaction
    Note over Controller, Valid: @Valid on request body
    Controller->>Valid: Validate request
    Valid-->>Controller: Exception
    Controller -->> Client: 400 Bad Request with Problem Details
    Client-->>User: Show field errors
```
#### Customer Not Found Error Path
```mermaid
sequenceDiagram
    actor User as Agent
    participant Client as Angular
    participant Controller as Spring Boot<br/>Controller
    participant Service as Spring Boot<br/>Service
    participant Repo as JPA Repository
    participant DB as PostgreSQL
    autonumber

    Service->>DB: BEGIN transaction
    Service->>+Repo: Find CUS-1001
    Repo->>+DB: SELECT for CUS-1001
    DB-->>-Repo: Empty result
    Repo-->>Service: Empty optional
    Service->>DB: ROLLBACK transaction
    Service-->>Controller: CustomerNotFoundException
    Controller-->>Client: 404 Not Found
```
#### Business Logic Violation Error Path
```mermaid
sequenceDiagram
    actor User as Agent
    participant Client as Angular
    participant Controller as Spring Boot<br/>Controller
    participant Service as Spring Boot<br/>Service
    participant Domain as Customer
    participant Repo as JPA Repository
    participant DB as PostgreSQL
    autonumber

    Repo-->>Service: Return Customer
    Service->>Domain: Add interaction
    Domain-->>Service: Business logic violation
    Service->>DB: ROLLBACK transaction
    Service-->>Controller: DomainException
    Controller-->>Client: 422 Unprocessable Entity
```
#### Persistence Error Path
```mermaid
sequenceDiagram
    actor User as Agent
    participant Client as Angular
    participant Controller as Spring Boot<br/>Controller
    participant Service as Spring Boot<br/>Service
    participant Repo as JPA Repository
    participant DB as PostgreSQL
    autonumber

    Service->>Repo: Save interaction
    Repo->>DB: INSERT interaction record
    alt Constraint violation
      DB-->>Repo: UK/FK violation
      Repo-->>Service: DataAccessException
      Service->>DB: ROLLBACK transaction
      Service-->>Controller: Wrap or propogate
      Controller-->>Client: 409 Conflict
    else Connection timeout
      DB--xRepo: Timeout, etc.
      Repo-->>Service: Connection Exception
      Service->>DB: ROLLBACK transaction
      Service-->>Controller: Wrap or propogate
      Controller-->>Client: 503 Service Unavailable
    end
```

### RecordCustomerInteraction UML Diagram
```mermaid
classDiagram
    namespace Domain {
        class CustomerStatus {
            <<enumeration>>
            ACTIVE
            PROSPECT
        }
        class Customer {
            <<Aggregate>>
            -UUID customerId
            -String fullName
            -String email
            -CustomerStatus status
            -List~CustomerInteraction~ interactions

            +getId() UUID
            +getName() String
            +getEmail() String
            +getStatus() CustomerStatus
            +getInteractions() List~CustomerInteraction~
            +activate() void
            +addInteraction(String channel, String summary, String actor, String correlationId) void
        }
        class CustomerInteraction {
            <<Aggregate>>
            -UUID interactionId
            -UUID customerId
            -String channel
            -String summary
            -String actor
            -Instant occurredAt
            -String correlationId

            +getInteractionId() UUID
            +getCustomerId() UUID
            +getChannel() String
            +getSummary() String
            +getActor() String
            +getOccurredAt() Instant
            +getCorrelationId() String
        }
    }

    class RecordInteractionController {
        -CustomerService service
        CustomerController(CustomerService service)
        recordInteraction() ResponseEntity
    }
    class RecordInteractionService {
        -CustomerRepository customerRepo
        -InteractionRepository interactionRepo
        -NotificationService notifier
        CustomerService(customerRepo, interactionRepo, notifier)
        recordInteraction(OnboardRequestDTO customer) OnboardResponseDTO
    }
    class CustomerRepository {
        <<interface>>
        save(Customer customer) Customer
        findById(UUID id) Customer
        exists(UUID id) boolean
    }
    class InteractionRepository {
        <<interface>>
        save(CustomerInteraction interaction) CustomerInteraction
        findById(UUID id) CustomerInteraction
        exists(UUID id) boolean
    }
    class JpaRepository {}
    RecordInteractionController --|> RecordInteractionService: delegates
    RecordInteractionService --|> CustomerRepository
    RecordInteractionService --|> InteractionRepository
    CustomerRepository --|> JpaRepository: extends
    InteractionRepository --|> JpaRepository: extends
    JpaRepository --|> Customer: persists
    JpaRepository --|> CustomerInteraction: persists
```

