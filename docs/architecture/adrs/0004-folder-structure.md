# Architectural Decision Record #0004
## Vertical Slice-based/Feature Folder Structure
2026-10-05

### Context
How to organize project for maintainability and developer experience

### Options
1. Vertical-Slice/Feature Based Structure
2. Layer-based Structure

### Decision
Choose to use a vertical-slice based folder structure
A vertical slice represents a single business operation encapsulated in one folder

Current Proposed Structure:
```plaintext
crm/
├── .github/    # GitHub Actions CI/CD workflows, CODEOWNERS
├── docs/                  
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/northstar/crm/
│   │   │   │   ├── common/                    # Cross-cutting
│   │   │   │   │   ├── config/
│   │   │   │   │   ├── exception/
│   │   │   │   │   └── security/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Customer.java
│   │   │   │   │   ├── CustomerStatus.java
│   │   │   │   │   └── security/
│   │   │   │   └── recordCustomerInteraction/           # Vertical Feature Slice
│	│	│   │       ├── Controller.java
│	│	│   │       ├── Repository.java
│	│	│   │       ├── Service.java
│	│	│   │       ├── Customer.java
│	│	│   │       ├── dto/     
│	│	│   │       ├── event/   
│	│	│   │       └── mapper/
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   └── app/
│   │       ├── core/       
│   │       ├── features/  
│   │       └── shared/   
│   └── package.json
└── infra/                    # Infrastructure as Code: Ansible, Terraform, Kubernetes
```
