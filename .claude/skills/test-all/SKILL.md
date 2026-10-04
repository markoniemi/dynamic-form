---
name: test-all
description: "Run frontend unit tests, backend unit tests, and backend integration tests, verify the test reports are fresh, then report a summary of all results."
---

# Test All (Unit + Integration)

Run full test suite: frontend unit tests, backend unit tests, backend integration tests.

Use the CLI (`npx`, `mvn`), not IntelliJ run configurations: it works without the IDE open and matches what CI runs.

## Test phases

Phases 1 and 2 are independent and can run in parallel. Paths are relative to the repository root.

**Phase 1: Frontend unit tests**
- Command: `cd frontend && npx vitest run`
- Framework: Vitest
- Includes: Component tests, utils tests

**Phase 2: Backend unit + integration tests**
- Command (from repository root): `mvn -pl backend -am verify`
- Surefire runs unit tests (`*Test`), failsafe runs integration tests (`*IT`, Spring Boot Test + Testcontainers; Docker must be running)
- `-am` also builds the frontend module, which the backend packages as a WebJar
- Do not run `mvn verify` or `mvn test` from inside `backend/`: it can report `BUILD SUCCESS` without running any tests

## Verify results

`BUILD SUCCESS` alone is not proof that tests ran. Before reporting:
1. Check the modification times of `backend/target/surefire-reports/*.txt` and `backend/target/failsafe-reports/*.txt` are from this run
2. Read the `Tests run:` lines from those report files for the counts

If the reports are stale, say so and do not report the backend as passing.

## On failure

Rerun a single failing test through IntelliJ MCP instead of the whole suite:
1. `mcp__idea__get_run_configurations` with `filePath` of the test class to get run points
2. `mcp__idea__execute_run_configuration` with `filePath` + `line` of the failing test method
3. To step through it, use `mcp__idea__xdebug_start_debugger_session` with breakpoints set via `mcp__idea__xdebug_set_breakpoint`

If IntelliJ is not available, fall back to `mvn -pl backend -am verify -Dtest=ClassName#method -Dsurefire.failIfNoSpecifiedTests=false` (for an IT use `-Dit.test=ClassName`).

## Reporting

Summary shows:
- Frontend: test files passed, tests passed
- Backend unit: tests passed/failed
- Backend integration: tests passed/failed
- All failing tests listed with error messages
- Overall status: ✓ all pass or ✗ failures

## When to use

- Before pushing to remote
- Verifying full functionality
- Regression testing
