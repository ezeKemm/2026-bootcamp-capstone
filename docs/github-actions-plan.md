# GitHub Actions plan

## Rules

1. **Package once.** CI tests and builds the artifacts. The package job reuses exactly those artefacts. A deploy job never rebuilds.
2. **Least privilege.** Workflows default to `permissions: contents: read`; a job asks for more only where it needs it (`packages: write` on the image job only).
3. **Pinned and linted.** Every third-party action is pinned to a full commit SHA with the version in a comment.
4. **Deployment ownership.** 

## Events and jobs

| Trigger | Jobs | Evidence |
| --- |----------------------------------------------------------------------------------------------------------------------------------------------|----------------------|
| `pull_request` | `pr-title` (Conventional Commits check); `backend`: `./mvnw -B -ntp verify`; `frontend`: `npm ci`, lint, `ng test --watch=false`, `ng build` | Reports, Actions logs |
| `push` to `main` | Everything above, then `image`: verify checksum, build images, scan them                                                                     | Artifacts            |
| `v*` tag | `deploy` to environment, requires reviewer approval (not who deployed)                                                                       | Environment          |

Runner: `ubuntu-24.04`

## Gates

| Gate                        | Fails                                                                    | Resolve                                                  |
|-----------------------------|--------------------------------------------------------------------------|----------------------------------------------------------|
| Backend tests               | Any test fails or is skipped without a recorded reason                   | Read report, fix code, push again; do not delete or skip |
| Frontend lint, tests, build | Any failure                                                              | Reproduce locally, fix code                              |
| SAST/CodeQL                 | High-severity finding                                                    | Fix; otherwise report                                    |
| Dependency scan             | Any High or Critical vulnerability                                       | Upgrade dependency or record report                      |
| Image scan                  | Any High or Critical vulnerability                                       | Update image, rebuild, rescan                            |
| PR title                    | Title does not follow the convention in `docs/team.md`                   | Edit PR title to match convention                        |
| Release manifest check      | Checksum or commit SHA does not match                                    | Cancel deploy, rerun pipeline                            |
| Smoke test                  | Readiness is not `UP`, or `CUS-1001` is not returned through the Route   | Trigger rollback, read log and fix in new commit         |