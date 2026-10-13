# GitHub Actions plan

Eight workflows in `.github/workflows/`. CI tests the code, Image builds and scans the images, CD deploys exact image digests to k3s. Decision record: [ADR 0001](architecture/adrs/0001-github-actions.md). Environments: [environment-strategy.md](environment-strategy.md).

## Rules

1. **Build once, deploy what passed.** CI tests the code. Image builds each image once on `main`, scans it by digest and tags it `passed-<commit>`. CD never builds; it deploys two digests and refuses a digest that is not in the registry.
2. **Least privilege.** Workflows default to `permissions: contents: read` or `permissions: {}`. A job asks for more only where it needs it: `packages: write` on the Image jobs, `packages: read` to resolve digests in CD, `security-events: write` for CodeQL.
3. **Pinned and linted.** Every third-party action is pinned to a full commit SHA with the version in a comment. `actionlint` checks the workflow files on every pull request. Dependabot raises weekly updates for Actions, Maven, npm and Docker.
4. **Deployment ownership.** Only the CD workflow deploys, and only to `dev` and `stage`. Each GitHub Environment holds its own cluster address, token and secrets. Stage is protected by a required reviewer who did not start the run (configured in GitHub, see [environment-strategy.md](environment-strategy.md)).
5. **Secrets stay out of the repo and the logs.** Secrets come from GitHub Environments, reach the deploy script as environment variables and are written to the cluster through a private temporary file.
6. **Failures are recorded.** A failed deploy rolls back automatically and the run still ends red. A gate failure is fixed in code, never skipped.

## Workflows

| Workflow | Trigger | Jobs | Evidence |
| --- | --- | --- | --- |
| `ci.yaml` (CI) | Pull request; push to `main`; `v*` tag | `backend-verify`: `./mvnw clean verify` (unit tests, integration tests, JaCoCo gate). `backend-sast`: SpotBugs with Find Security Bugs, plus SBOM. `backend-package` (`main` and tags only): jar with `SHA256SUMS`. `frontend-verify`: `npm ci`, production `ng build`, `npm test` | Test, SAST and SBOM reports; `crm-jar` artifact |
| `image.yaml` (Image) | After CI succeeds for a push to `main` | `build` (backend and frontend): multi-stage Docker build, push to GHCR as `sha-<commit>`, Trivy scan by digest. `promote`: tags both images `passed-<commit>` and prints the digests | Job summary with the two digests |
| `cd.yaml` (CD) | Manual run (environment and two digests); `v*` tag | `resolve`: pick the environment and digests, check them in the registry. `deploy`: write the kubeconfig, check access, deploy by digest, smoke test, roll back on failure | Job summary; the Environment's deployment history |
| `codeql.yaml` (CodeQL) | Pull request; push to `main`; weekly (Monday); manual | `analyze` for Java and the frontend language set in the matrix | Security tab, code scanning |
| `dependency-scan.yaml` (Dependency) | Pull request; push to `main`; `v*` tag | OWASP Dependency-Check through Maven. Runs only when the repository variable `RUN_DEP_SCAN` is `true`; needs the `NVD_API_KEY` secret | `dependency-scan-report` artifact |
| `infra.yaml` (Infra) | Pull request and push to `main` that change `infra/**` | `terraform fmt -check`, `init -backend=false`, `validate`. No AWS credentials. Plan and apply run by hand | Actions log |
| `pr-title.yaml` (PR Title) | Pull request | Conventional Commits check on the title | Actions log |
| `actionlint.yaml` (actionlint) | Pull request; push to `main` | Lint of all workflow files | Actions log |

Runner for every job: `ubuntu-24.04`.

## Deploy paths

| Path | Starts with | Goes to | Approval |
| --- | --- | --- | --- |
| Manual | Actions, CD, Run workflow: choose `dev` or `stage`, paste the two digests from the Image job summary | The chosen environment | Required reviewer on `stage` (GitHub Environment setting) |
| Tag | Push a `v*` tag on a commit whose images already carry `passed-<commit>` | `dev` | None |

Both paths run the same deploy steps: kubeconfig, access check, deploy in tiers (config, data, application), smoke test, and on failure an automatic rollback to the previous revision. Pushes to one environment queue instead of cancelling each other.

## Gates

| Gate | Fails | Resolve |
| --- | --- | --- |
| Backend tests | Any unit or integration test fails | Read the report, fix the code, push again. Do not delete or skip a test |
| Coverage | JaCoCo line coverage below 80% on services and domain | Add the missing tests |
| Frontend build and tests | `ng build` or any test fails | Reproduce locally with the same commands, fix the code |
| SAST (SpotBugs + Find Security Bugs) | A finding at High threshold | Fix; otherwise record it in the [risk register](risk-register.md) |
| CodeQL | A finding in code scanning. Blocks a merge only if branch protection requires the check | Fix, or dismiss with a written reason |
| Dependency scan | A CVE with CVSS 7 or higher (when `RUN_DEP_SCAN` is on) | Upgrade the dependency or record an accepted risk with an owner and date |
| Image scan (Trivy) | A High or Critical finding that has a fix. Findings without a fix are ignored | Update the base image or dependency, rebuild, rescan. Suppressions go in `.trivyignore.yaml` with a reason |
| Digest check | A digest has a bad format or is not in the registry | Copy the digests again from the Image job summary |
| Terraform | `fmt`, `init` or `validate` fails | Run `terraform fmt` and `validate` locally |
| PR title | The title does not follow the convention in [team.md](team.md) | Edit the pull request title |
| Workflow lint | `actionlint` reports an error | Fix the workflow file |
| Smoke test | Readiness is not `UP`, login fails, or the seeded customer (Amina Khan) is not returned through the Ingress | Rollback runs by itself; read the log and fix in a new commit |

## Known gaps

Tracked in the [risk register](risk-register.md).

- **The jar from CI is not reused.** `backend-package` stores a checksummed jar, but the Image workflow rebuilds from source inside the Dockerfile. Rule 1 holds for images (built once, scanned and deployed by digest) and not for the jar. A TODO in `ci.yaml` records it.
- **Tag deploys skip approval.** A `v*` tag deploys to `dev` with no reviewer. `stage` needs one only if the Environment is configured that way.
- **The dependency scan can be off.** It runs only when `RUN_DEP_SCAN` is `true`.
- **No frontend lint in CI.** `npm run lint` exists but no job runs it.
- **No Terraform plan in CI.** Pipelines never see AWS credentials, so a person reviews each plan.
- **No runbook.** `cd.yaml` and `rollback.sh` refer to a runbook that is not in the repo.