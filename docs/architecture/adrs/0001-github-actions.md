# Architectural Decision Record #0001
## GitHub Actions for CI/CD
2026-10-05

### Context
How to automatically manage development cycle for better reliability, maintainability, and customer satisfaction?
Require a CI/CD pipeline to manage commits to repository, testing, and delivery

### Options
1. GitHub Actions
2. BitBucket Pipelines

#### Decision
Choose GitHub Actions to run all CI/CD for easy integration into existing GitHub repository.
All third-party actions are pinned to a full commit SHA

