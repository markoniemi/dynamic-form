Run frontend unit tests (Vitest) and backend unit tests (JUnit, no integration tests), then report a summary of results.

Run from the repository root. The Maven `test` phase runs surefire only, so integration tests (`*IT`) are not run. Do not run `mvn` from inside `backend/`: it can report `BUILD SUCCESS` without running any tests. `-Dskip.npm` skips the frontend module's `npm run ci`, since Vitest runs separately here.

```bash
FE=$(mktemp); LOG=$(mktemp)
(cd frontend && npx vitest run > "$FE" 2>&1); fe=$?
grep -E "Test Files|Tests |FAIL|Error" "$FE" | tail -20
mvn -pl backend -am test -Dskip.npm > "$LOG" 2>&1; status=$?
grep -E "Tests run:|FAIL|ERROR|BUILD" "$LOG" | tail -40
echo "vitest exit: $fe, mvn exit: $status; logs: $FE $LOG"
```

Trust the exit codes, not the filtered text. On a failure, read the relevant part of the log file instead of rerunning.

Before reporting, check that `backend/target/surefire-reports/*.txt` were modified by this run. If they are stale, say so and do not report the backend as passing.

After running, summarize:
- How many frontend tests passed / failed / skipped
- How many backend tests passed / failed / skipped
- List any failing tests with their error messages
- If all tests pass, confirm with a single line
