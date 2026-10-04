---
name: test-all
description: "Run frontend unit tests, backend unit tests, and backend integration tests, verify the test reports are fresh, then report a summary of all results."
---

Run frontend unit tests (Vitest), backend unit tests (surefire) and backend integration tests (failsafe, Testcontainers; Docker must be running), then report a summary of results.

Run from the repository root. Do not run `mvn` from inside `backend/`: it can report `BUILD SUCCESS` without running any tests.

```bash
(cd frontend && npx vitest run 2>&1); mvn -pl backend -am verify 2>&1 | grep -E "Tests run:|FAIL|ERROR|BUILD"
```

Before reporting, check that `backend/target/surefire-reports/*.txt` and `backend/target/failsafe-reports/*.txt` were modified by this run. If they are stale, say so and do not report the backend as passing.

After running, summarize:
- How many frontend tests passed / failed / skipped
- How many backend unit and integration tests passed / failed / skipped
- List any failing tests with their error messages
- If all tests pass, confirm with a single line
