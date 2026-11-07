---
id: fix-stale-skill-facts
status: in_review
priority: high
---

Finding C-1 of `docs/ContextAuditFindings.md`. `dynamic-form-backend/SKILL.md`
claims Spring Boot 3.5.6, Java 17+, H2 for dev and a `dto/` package; actual is
4.0.3, Java 21, Postgres for dev, DTOs in `api`. It also tells the agent to
`cd backend; mvn test`, which `.claude/commands/test.md` explicitly forbids. Two
wrong paths as well: `config/GlobalExceptionHandler.java` in the fullstack skill
and `src/context/oidcConfig.tsx` in the frontend skill.

# Implementation Plan

Facts checked against the code before writing any replacement:

| Claim in skill | Reality | Source |
| :- | :- | :- |
| Spring Boot 3.5.6 | 4.0.3 | `backend/pom.xml` parent version |
| Java 17+ | 21 | `backend/pom.xml` `<java.version>` |
| H2 for Dev and Test | Postgres for dev, H2 for test only | `backend/CLAUDE.md:14` |
| `application-dev.yaml` is H2 | dev profile is Postgres | same |
| backend `dto/` package | DTOs are `com.example.dynamicform.api` in the `api` module (6 classes) | glob of `api/src/main/java/...` |
| `config/GlobalExceptionHandler.java` | `controller/GlobalExceptionHandler.java` | grep of `backend/.../controller` |
| `src/context/oidcConfig.tsx` | `src/types/oidcConfig.ts` | glob of `frontend/src` |
| `cd backend; mvn test` | `mvn -pl backend test` | `.claude/commands/test.md:3` forbids running Maven from inside `backend/` |

Also confirmed to exist, so they stay as written: `DevelopmentApplication` and
`IntegrationTestBase` in `backend/src/test/java/com/example/backend/`, and all
four `application*.yaml` profiles.

Steps:

1. `dynamic-form-backend/SKILL.md` — drop the hardcoded framework and language
   versions (the module `CLAUDE.md` owns them, which is why they drifted), fix
   the dev/test database split, replace the `dto/` entry with a pointer to the
   `api` module, and change every Maven command to `-pl backend` form.
2. `dynamic-form-fullstack/SKILL.md` — fix the `dto/` table row and the
   `GlobalExceptionHandler` path; fix the `mvn test` command in the workflow.
3. `dynamic-form-frontend/SKILL.md` — fix the `oidcConfig` path.

Scope limit: this task fixes wrong facts only. The generic style advice (C-2) and
the duplicated error table and project maps (C-8) are their own tasks and are left
alone here.

No unit tests: the change edits instruction prose. Verification is grepping the
skills for the stale strings.

# Outcome

Done, three files changed:

* `dynamic-form-backend/SKILL.md` — versions replaced by a pointer to
  `backend/CLAUDE.md`; dev/test database split corrected (Postgres for dev, H2
  for test only), with the Flyway rule added since it follows from it; `dto/`
  replaced by a line saying DTOs are in the `api` module; all three Maven
  commands changed to `-pl backend` form, with the reason `cd backend` is wrong
  stated in place; `application-prod.yaml` added to the resources list, which was
  missing.
* `dynamic-form-fullstack/SKILL.md` — `dto/` fixed in both the key-files table
  and the endpoint checklist; `config/GlobalExceptionHandler.java` →
  `controller/`; `mvn test` → `mvn -pl backend test`; the workflow's step 2 now
  says DTO changes start in the `api` module.
* `dynamic-form-frontend/SKILL.md` — `src/context/oidcConfig.tsx` →
  `src/types/oidcConfig.ts`.

Verified: grepping `.claude/skills` for `dto/`, `3.5.6`, `Java 17`,
`config/GlobalException` and `context/oidcConfig` returns nothing, and every
remaining `mvn` invocation in the skills is either `-pl` scoped or a
`dependency:tree` example.

One stale string was missed on the first pass — the `dto/` in the endpoint
checklist at line 120 — and caught by that grep. The grep is the check that
matters here, not reading the diff.
