# Architectural Decision Record #0002
## PostgreSQL for Persistence
2026-10-05

### Context
How to reliably persist application data?

### Options
1. PostgreSQL
2. Oracle

#### Decision
Choose PostgreSQL for database persistence as it is open-source, reliable, and well-supported.
Will use PostgreSQL 16 as the system of record for all data storage needs.
Managed using Flyway and accessed using Spring JPA 