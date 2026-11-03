---
name: test-all
description: "Run frontend unit tests, backend unit tests, and backend integration tests, verify the test reports are fresh, then report a summary of all results."
---

Run frontend unit tests (Vitest), backend unit tests (surefire) and backend integration tests (failsafe, Testcontainers; Docker must be running), then report a summary of results.

Run from the repository root. Do not run `mvn` from inside `backend/`: it can report `BUILD SUCCESS` without running any tests. The `-am` build runs the frontend module's `npm run ci`, which includes Vitest, so there is no separate Vitest run.

```bash
LOG=$(mktemp)
mvn -pl backend -am verify > "$LOG" 2>&1; status=$?
grep -E "Test Files|Tests  |Tests run:|FAIL|ERROR|BUILD" "$LOG" | tail -60
echo "mvn exit: $status; log: $LOG"
```

Trust the exit code, not the filtered text. On a failure, read the relevant part of the log file instead of rerunning.

Before reporting, check that `backend/target/surefire-reports/*.txt` and `backend/target/failsafe-reports/*.txt` were modified by this run. If they are stale, say so and do not report the backend as passing.

After running, summarize:
- How many frontend tests passed / failed / skipped (the `Tests  N passed` line)
- How many backend unit and integration tests passed / failed / skipped
- List any failing tests with their error messages
- If all tests pass, confirm with a single line
