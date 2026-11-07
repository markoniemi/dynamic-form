---
name: test
description: "Run this project's tests and report a summary. Unit tests only by default (frontend Vitest + backend surefire); pass --with-integration to add the backend integration tests (failsafe, Testcontainers, Docker required)."
---

Run from the repository root. Never run `mvn` from inside `backend/`: it can report `BUILD SUCCESS` without running any tests.

Commands are PowerShell, because the `Bash` tool is denied in this environment.

## Unit tests only (default)

`-Dskip.npm` skips the frontend module's `npm run ci`, since Vitest runs separately here. Keep it quoted — PowerShell otherwise splits it at the dot and Maven reads `.npm` as a lifecycle phase.

```powershell
$fe = New-TemporaryFile; $log = New-TemporaryFile
Push-Location frontend; npx vitest run > $fe.FullName 2>&1; $feStatus = $LASTEXITCODE; Pop-Location
Select-String -Path $fe.FullName -Pattern 'Test Files|Tests |FAIL|Error' | Select-Object -Last 20
mvn -pl backend -am test "-Dskip.npm" > $log.FullName 2>&1; $mvnStatus = $LASTEXITCODE
Select-String -Path $log.FullName -Pattern 'Tests run:|FAIL|ERROR|BUILD' | Select-Object -Last 40
"vitest exit: $feStatus, mvn exit: $mvnStatus"
"logs: $($fe.FullName) $($log.FullName)"
```

## With integration tests (`--with-integration`)

Docker must be running. The `-am` build runs the frontend module's `npm run ci`, which includes Vitest, so there is no separate Vitest run here.

```powershell
$log = New-TemporaryFile
mvn -pl backend -am verify > $log.FullName 2>&1; $mvnStatus = $LASTEXITCODE
Select-String -Path $log.FullName -Pattern 'Test Files|Tests  |Tests run:|FAIL|ERROR|BUILD' | Select-Object -Last 60
"mvn exit: $mvnStatus; log: $($log.FullName)"
```

## Reporting

Trust the exit codes, not the filtered text. On a failure, read the relevant part of the log file instead of rerunning.

Before reporting, check that `backend/target/surefire-reports/*.txt` — and `backend/target/failsafe-reports/*.txt` when integration tests ran — were modified by this run. If they are stale, say so and do not report the backend as passing.

Then summarize:

- How many frontend tests passed / failed / skipped
- How many backend unit tests, and integration tests when run, passed / failed / skipped
- Any failing tests, with their error messages
- If everything passes, confirm in a single line
