Run frontend unit tests (Vitest) and backend unit tests (JUnit, no integration tests), then report a summary of results.

Run from the repository root. The Maven `test` phase runs surefire only, so integration tests (`*IT`) are not run. Do not run `mvn` from inside `backend/`: it can report `BUILD SUCCESS` without running any tests.

```bash
(cd frontend && npx vitest run 2>&1); mvn -pl backend -am test 2>&1 | grep -E "Tests run:|FAIL|ERROR|BUILD"
```

Before reporting, check that `backend/target/surefire-reports/*.txt` were modified by this run. If they are stale, say so and do not report the backend as passing.

After running, summarize:
- How many frontend tests passed / failed / skipped
- How many backend tests passed / failed / skipped
- List any failing tests with their error messages
- If all tests pass, confirm with a single line
