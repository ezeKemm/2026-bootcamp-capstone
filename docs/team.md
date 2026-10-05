## Overview
This is a living document for all the working agreements of the team and will outline team structure, expectations, and a Definition of Done for the capstone project.

## Purpose and Scope

The capstone project is intended to provide hand-on experience building enterprise-level software by implementing an end-to-end vertical slice of Northstar Bank’s CRM platform, responsible for recording interactions between the bank and it’s customers.
The project covers all elements of operating in an enterprise team, including documentation, infrastructure, and implementation of the entire software architecture, from frontend to backend, with all security, observability, and testing capabilities included.

## Roles

Team Lead: Ethan Kish
Technical Lead: Ezekiel Maroclo-Kemmerling
Documentation Lead: Joel Hoskins
Presentation Lead: Gloria Ma

## Working Agreements

### Branch Management
1. Trunk Based: Work is done in short-lived branches off `main` . Everyone makes small, regular commits to `main` with incremental updates, managed by CI/CD pipeline to keep `main` releasable.
2. Naming: Branch names follow `<branch-type>/<short-topic>`  pattern; lower-case, hyphens, no names or commit tags; follow conventional commit types (see below); examples: `feat/customer-model` , `fix/controller-validation` , `build/build-script` .
3. Merging: Squash merge only; all commits on a branch are squashed into one commit to `main` ; pull request title becomes commit message and branch is deleted
4. Branch Protection: No direct commits to `main` are allowed; no force-pushes of shared branches

### Commits
5. [Conventional Commits](https://gist.github.com/qoomon/5dfcdf8eec66a051ecd85625518cfd13): commits to main follow `type(scope): summary` ; the scope and body are optional; written in imperative mood; no punctuation to end; keep short and concise.
6. Commit types: `feat` , `fix` , `docs` , `refactor` , `test` , `build` , `ci` , `chore` , `perf`
7. Commit scopes: `backend` , `frontend` , `openshift` , `infra` , `ci` , `docs` .
8. Breaking changes are marked by ! after scope; added context is described in `BREAKING CHANGE:` footer in commit message.
9. Never put secrets, sensitive and real data (PII) , or internal notes in commit messages.
10. Validate the PR title for the squashed commits meets convention, not every commit message.

### Pull Requests
11. Every change to `main` is done through a pull request (PR) using the PR template.
12. Each PR requires one approving review from someone who did not author the code before merging; enforced by GitHub Rulesets
13. PRs remain small and quick to review. Each PR is reviewed within the day; the backlog of all PRs not reviewed by end of day must be reviewed before any new PRs by next day.
14. Review comments remain focused on the code, detailing what to change and how; conversations must be resolved before merging.
15. Any change involving AI-assistance must be explicitly noted and follow `docs/ai-usage-plan.md`; the technical lead must approve all PRs involving AI-assistance.
16. A PR that is not green (does not pass all required checks) cannot be merged.

### Communication, Documentation, and Project Management
17. Project-scoped communication remains centralized in the Slack channel.
18. The team uses Notion to assist in project management, including a task board and a place to draft documentation; tasks in Notion are maintained daily.
19. Decisions about architecture are an ADR under `docs/adrs/`, decisions on how the team works are a working agreement under this document `docs/team.md`.
20. Changes to decisions are proposed as PRs which all team members must approve. A decision is final once the change is committed to `main`.
21. Team members are expected to not hesitate to voice when they are struggling or have a concern and should expect constructive support or discussion.
22. Ownership: Each part of the project has an owner and a reviewer.

### Definition of Done
- [ ]  `POST` for Amina returns 201 and a Location header
- [ ]  Invalid body returns 400 Problem details and does not persist or publish
- [ ]  CUS-9999 returns 404 and does not persist or publish
- [ ]  Flyway migration applied; row exists; generated SQL inspected once
- [ ]  One `CustomerInteractionRecordedV1` per successful create, keyed by customer id, correlation id present
- [ ]  Consumer dedupes on `eventId`, retries are bounded, poison messages to the a DLT (dead letter topic)
- [ ]  Unit, MockMvc, JPA, and Kafka testx green twice
- [ ]  docs/backend-demo.md lets a peer reproduce it with no verbal coaching
- [ ]  No secrets in the repo, no passwords in the code, no PII in the fixtures, no credentials in the demo.http