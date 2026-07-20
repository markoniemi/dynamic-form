# Automated Technical Debt Reduction — Plan

## Overview

Use an on-premises AI to systematically reduce technical debt during nights and
weekends. The work happens in small, safe, automated steps. Each step produces a
branch and pull request that humans review every morning. CI must pass before any
PR is considered for merge.

The system covers two types of work, run through the same pipeline:

- **Test generation** — adding missing unit tests (Jest, JUnit) and Robot Framework
  e2e tests. This is the safer starting point: a bad test fails immediately and
  can never break production.
- **Refactoring** — small structural improvements flagged by static analysis.
  Start this only after the test generation process is stable, so the test suite
  is stronger before the bot touches production code.

The system is built incrementally — humans prove each phase works before handing
autonomy to the next phase.

---

## Files

All files live in `debt-bot/` in the repository root, committed to `main`.

### FindingsBackendTests.md
AI writes this from JaCoCo coverage reports. Lists untested service methods,
controller endpoints, and exception paths with risk/impact scores.
Refreshed monthly or after major releases.

### FindingsFrontendTests.md
AI writes this from Jest/Vitest coverage reports. Lists untested React components
and user interactions with risk/impact scores.

### FindingsE2eTests.md
AI writes this by comparing all known routes and API endpoints against existing
Robot Framework test files. Lists missing user journeys.

### FindingsRefactoring.md
AI writes this from linter, complexity, and churn reports. Lists code smell
findings with risk/impact scores. Not used until test generation is stable.

### Tasks.md
Human and AI write this together. Bot ticks checkboxes.
Contains one instruction card per task in priority order, tagged by type.
This is the single source of truth for what needs doing and where things stand.

### Status.md
Bot writes this. Humans read it for debugging.
Append-only log of every nightly run — what was attempted, what passed, what failed.

---

## Checkbox Convention (Tasks.md)

Tasks are tagged by type. The bot uses the tag to select the right prompt template.

```markdown
## TASK-NNN · [backend-test] Add test for UserService.findByEmail
## TASK-NNN · [frontend-test] Add test for LoginForm error state
## TASK-NNN · [e2e] Add Robot test for login error flow
## TASK-NNN · [refactor] Extract timeout constants in validator.js
```

Each task block follows this exact format:

```markdown
## TASK-NNN · [type] Short description
> Context: why this matters, which analysis signal flagged it
> File: src/path/to/file.java  (for test tasks: the NEW test file to create)
> Source file: src/path/to/Subject.java  (for test tasks: the file being tested)
> Test command: ./mvnw test -Dtest=UserServiceTest
> Style reference: src/test/java/.../OrderServiceTest.java  (match this pattern)

- [x] planned
- [ ] branch created · `debt/task-NNN-short-slug`
- [ ] pr created · <url>
- [ ] ci passed
- [ ] merged
- [ ] rejected
<!-- attempts: 0 | last_error: none -->
```

Rules:
- `planned` is ticked by a human to signal the task is ready to execute
- All other checkboxes are ticked by the bot
- The PR url is written inline by the bot when the PR is opened
- `merged` and `rejected` are mutually exclusive — bot syncs these from GitLab/GitHub
- The HTML comment is bot-maintained metadata — do not edit manually
- Humans may reorder task blocks to reprioritize
- Humans may edit task descriptions and instructions freely
- Do not tick checkboxes manually

---

## Analysis — What to Collect

### Test Gap Analysis (run first)

**Backend — JaCoCo:**
```bash
./mvnw test jacoco:report
# Reports in target/site/jacoco/jacoco.xml
```
Feed to AI: *"List all service methods and controller endpoints with 0% coverage.
Then list methods with uncovered exception/error branches. Score by impact
(how critical is this code path) and risk (low — adding a new test file is always low risk)."*

**Frontend — Jest/Vitest:**
```bash
npm test -- --coverage --coverageReporters=json-summary
```
Feed to AI: *"List components with no tests at all, then components with low branch
coverage. For each, describe what user interactions are untested."*

**E2E — Robot gap analysis:**
E2E coverage cannot be measured by line coverage tools. Collect instead:
```bash
# All frontend routes
grep -r "path:" src/router  >  debt-bot/raw/Routes.txt

# All backend endpoints
grep -r "@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping" src/main \
  > debt-bot/raw/Endpoints.txt

# All existing Robot test files
find tests/robot -name "*.robot" > debt-bot/raw/RobotTests.txt
```
Feed all three to AI: *"Compare the routes and endpoints against the existing Robot
tests. List user journeys and API endpoints with no E2E test coverage."*

### Refactoring Analysis (run after test generation is stable)

```bash
# Git churn
git log --format=format: --name-only | sort | uniq -c | sort -rg > debt-bot/raw/Churn.txt

# Linter
eslint . --format json > debt-bot/raw/lint.json        # frontend
./mvnw checkstyle:check > debt-bot/raw/checkstyle.txt  # backend

# Complexity
npx complexity-report --format json src/ > debt-bot/raw/complexity.json
```

---

## Scoring Matrix

Tasks are scored on two axes before curation.

**Risk** (how likely is this to break something):
- Test tasks: always low — new files cannot break existing behaviour
- Refactor tasks: lines changed, files touched, coverage of affected code,
  whether it touches a public interface, churn rate of the file

**Impact** (how much value does this add):
- Test tasks: how critical is the untested code path; is it on a high-churn file
- Refactor tasks: SonarQube severity, complexity score, churn rate

**Recommendation table:**

| Risk   | Impact | Recommendation               |
|--------|--------|------------------------------|
| low    | high   | ✅ automate — do first        |
| low    | low    | ✅ automate — good for tuning |
| medium | high   | 👁 human does manually        |
| high   | any    | ❌ skip — not for automation  |

Only `✅ automate` tasks go into Tasks.md for the bot.

**Suggested order across all task types:**

```
1. Backend unit tests    ← JaCoCo gives exact line numbers, risk always low
2. Frontend unit tests   ← component tests, still safe, new files only
3. E2E happy paths       ← high value, human reviews carefully
4. E2E error paths       ← need most human judgment on what matters
5. Refactoring           ← only after test suite is meaningfully stronger
```

---

## Instruction Card Quality Checklist

Before adding any task to Tasks.md, verify:

| Check | Good | Bad |
|---|---|---|
| Scope | Single file, <50 lines | Multiple files, architectural change |
| Instructions | Step-by-step, no judgment needed | "Improve the error handling" |
| Verification | Exact test command given | "Make sure tests pass" |
| Reversibility | Easy to discard the branch | Touches DB, migrations, public API |
| Context | Explains why, not just what | Instructions with no rationale |
| Size | Can be done in one AI call | Requires iterative back-and-forth |

For **test tasks** specifically also verify:
- The file to create is named explicitly (do not let the bot choose)
- A style reference file is given (so tests match the project's existing patterns)
- The exact scenarios to test are listed (happy path, error path, edge cases)
- For Robot tests: each step of the user journey is spelled out

---

## Phases

### Phase 1 — Manual Trial

**Goal:** Prove the instruction card format works before any automation.

**Exit criterion:** 5 tasks completed manually with zero judgment calls needed
during execution. Instruction cards are unambiguous.

Steps:

1. Run test gap analysis (see Analysis section above). Collect JaCoCo, Jest
   coverage, routes, endpoints, and existing Robot files into `debt-bot/raw/`.

2. Feed reports to AI to produce the three FINDINGS files. Review with the team.

3. Human and AI review findings together. Select 5–10 `✅ automate` tasks,
   prioritising backend unit tests first. AI writes the full instruction cards.
   Human pastes them into Tasks.md and ticks `[x] planned`.

4. Execute each task manually, treating yourself as the robot:
   - Follow the git flow exactly (see Git Flow section below)
   - Follow the instruction card literally — no improvisation
   - Note every place you had to make a judgment call

5. After each task, rewrite the instruction card to eliminate every judgment call.
   A card is done when another person could execute it blindly.

6. Open real PRs. Observe CI. Merge or close. Update Tasks.md checkboxes by hand
   for this phase only.

---

### Phase 2 — Semi-Automated

**Goal:** Bot makes the code change. Human still reviews the diff before pushing.

**Exit criterion:** Bot produces correct output for 10 consecutive tasks with no
human corrections needed before pushing.

Steps:

1. Build `Orchestrator.py`. For each task it:
   - Reads next pending task from Tasks.md
   - Reads the target file (or notes the file to create for test tasks)
   - Calls on-prem AI with task card + file content using the correct prompt template
   - Shows human the diff (or the new file)
   - Human approves or rejects
   - If approved: runs the git flow, ticks checkboxes, commits state to main
   - If rejected: logs rejection reason, increments attempts counter

2. Use two prompt templates in `debt-bot/prompts/`:

   **test.txt** — for all test task types:
   ```
   You are an automated test writer. Create a test file for the scenario
   described. Match the style of the reference file exactly. Do not test
   anything not listed in the task. Do not modify any existing files.

   TASK:
   {task_card}

   SOURCE FILE CONTENT:
   {source_file_content}

   STYLE REFERENCE:
   {style_reference_content}

   Respond with ONLY the complete new test file content. Nothing else.
   ```

   **refactor.txt** — for refactoring tasks:
   ```
   You are an automated refactoring agent. Complete this task exactly as
   specified. Do not make any changes beyond the instructions. Do not add
   comments. Do not reformat unrelated code.

   TASK:
   {task_card}

   CURRENT FILE CONTENT:
   {file_content}

   Respond with ONLY the complete updated file content. Nothing else.
   ```

3. Run manually (not via cron) for the first two weeks.

4. Track rejection reasons in Status.md. Improve prompt templates after each rejection.

---

### Phase 3 — Fully Automated Nightly Run

**Goal:** Bot runs unattended. Humans review PRs every morning.

**Exit criterion:** Two weeks of nightly runs with no bad PRs reaching review
(CI catches regressions, bot handles failures gracefully).

Steps:

1. Set up cron to run every 30 minutes during nights and weekends:
   ```
   */30 22-23,0-5 * * 1-5  /opt/debt-bot/run.sh
   0    8-20      * * 6-7   /opt/debt-bot/run.sh
   ```

2. Each cron invocation does exactly one task — see Git Flow and Orchestrator
   sections below for the precise sequence.

3. Wake/sleep the machine around the cron window:
   ```bash
   # Wake at 21:55, shutdown at 06:05
   sudo rtcwake -m no -t $(date -d "today 21:55" +%s)
   # In cron:
   5 6 * * 1-5 root /sbin/shutdown -h now
   ```

4. Morning ritual (15 minutes, whole team):
   - Open Tasks.md — see what ran overnight
   - Review open PRs — all have green CI already
   - Approve obvious ones, close bad ones
   - Add rejection reason as a PR comment for prompt tuning

---

## Git Flow

Every task follows this exact sequence. Order matters — deviating causes state corruption.

```
1.  git checkout main
2.  git pull origin main          ← always start from clean main
3.  read Tasks.md                 ← find next pending task
4.  read source/target file       ← capture current content before branching
5.  check remote branch absence   ← idempotency guard, exit if already exists
6.  git checkout -b debt/task-NNN ← create task branch
7.  AI produces file content      ← new test file or modified source file
8.  write file to disk
9.  run test command              ← verify before committing anything

    ── if tests FAIL ──────────────────────────────────────────
10. git checkout .                ← discard all changes
11. git checkout main
12. git pull origin main
13. update Tasks.md               ← increment attempts, log error
14. append Status.md
15. git add debt-bot/Tasks.md debt-bot/Status.md
16. git commit -m "debt-bot: TASK-NNN failed attempt N"
17. git push origin main
18. exit
    ───────────────────────────────────────────────────────────

    ── if tests PASS ──────────────────────────────────────────
10. git add <changed or new file only>
11. git commit -m "debt-bot: TASK-NNN short description"
12. git push origin debt/task-NNN
13. open PR via API               ← debt/task-NNN → main
14. git checkout main             ← switch back before touching state files
15. git pull origin main          ← ensure still clean
16. update Tasks.md               ← tick branch created + pr created + url
17. append Status.md
18. git add debt-bot/Tasks.md debt-bot/Status.md
19. git commit -m "debt-bot: TASK-NNN pr opened"
20. git push origin main
    ───────────────────────────────────────────────────────────
```

**Key rules:**
- Tasks.md and Status.md are NEVER committed to the task branch — only to main
- Always switch back to main before the state commit (step 14)
- Always pull main again before the state commit (step 15) — another process may
  have pushed since step 2
- The branch-exists check (step 5) makes every run recoverable — a crash anywhere
  is safe to retry

---

## Nightly Orchestrator Logic (pseudocode)

```python
def run():
    git("checkout main")
    git("pull origin main")
    sync_pr_statuses()              # tick merged/rejected in Tasks.md

    if count_open_prs() >= MAX_PRS:
        log("PR limit reached, exiting")
        commit_state_files()
        return

    task = find_next_pending_task() # planned, no branch, attempts < 3
    if not task:
        log("No pending tasks")
        return

    branch = f"debt/{task.id}-{slugify(task.title)}"
    if remote_branch_exists(branch):
        log(f"SKIP: branch already exists for {task.id}")
        return

    # build AI prompt based on task type
    if task.type in ("backend-test", "frontend-test", "e2e"):
        source_content = read_file(task.source_file)
        style_content  = read_file(task.style_reference)
        new_content    = call_ai("test", task.card, source_content, style_content)
        target_file    = task.file          # new file to create
    else:  # refactor
        current_content = read_file(task.file)
        new_content     = call_ai("refactor", task.card, current_content)
        target_file     = task.file         # existing file to overwrite

    write_file(target_file, new_content)
    result = run_tests(task.test_command)

    if result.failed:
        git("checkout .")
        git("checkout main")
        git("pull origin main")
        task.increment_attempts(result.error)
        update_tasks_md(task)
        append_status_md(task, "FAILED", result.error)
        commit_state_files(f"debt-bot: {task.id} failed attempt {task.attempts}")
        return

    git(f"checkout -b {branch}")
    git(f"add {target_file}")
    git(f'commit -m "debt-bot: {task.id} {task.title}"')
    git(f"push origin {branch}")
    pr_url = open_pr(branch, task)

    git("checkout main")
    git("pull origin main")
    task.tick("branch_created", branch)
    task.tick("pr_created", pr_url)
    update_tasks_md(task)
    append_status_md(task, "SUCCESS", pr_url)
    commit_state_files(f"debt-bot: {task.id} pr opened")
```

---

## File Structure

```
debt-bot/
  FindingsBackendTests.md    ← from JaCoCo analysis
  FindingsFrontendTests.md   ← from Jest/Vitest coverage
  FindingsE2eTests.md        ← from route/endpoint vs Robot file comparison
  FindingsRefactoring.md      ← from linter + complexity + churn (use later)
  Tasks.md                     ← task queue and progress dashboard
  Status.md                    ← append-only nightly run log
  Orchestrator.py              ← main loop
  GitlabClient.py             ← PR creation and status sync
  AiClient.py                 ← on-prem AI calls
  TestRunner.py               ← run tests, parse pass/fail
  prompts/
    test.txt                   ← prompt template for test generation tasks
    refactor.txt               ← prompt template for refactoring tasks
    analysis.txt               ← prompt for generating FINDINGS files
  raw/                         ← static analysis output, not committed
    jacoco.xml
    CoverageSummary.json
    Routes.txt
    Endpoints.txt
    RobotTests.txt
    Churn.txt
  logs/
    2026-05-22.log
```

---

## Build Order

Build and validate each piece before the next:

1. **TestRunner.py** — reliably detect pass/fail for all three test suites
   (Jest, Maven/JUnit, Robot). This is the safety guarantee everything depends on.

2. **GitlabClient.py** — open a dummy PR, sync its status, close it.

3. **AiClient.py** — send a task card + source file, get back a new test file.
   Verify the output compiles and the test runner accepts it.

4. **Phase 1** — manual trial with 5 backend test tasks. Tune instruction cards.

5. **Orchestrator.py** — wire the above together. Run manually with one task.

6. **Phase 2** — human-in-the-loop runs across all task types. Tune prompts.

7. **Cron + wake/sleep** — only after Phase 2 is stable.

8. **Refactoring tasks** — add to Tasks.md only after the test suite is
   meaningfully stronger from phases 1–7.

---

## Failure Handling

| Situation | Bot action |
|---|---|
| Tests fail | Discard changes, increment attempts, log error, move to next task |
| AI returns garbage | Tests will catch it — treat as test failure |
| Branch already exists | Skip task, log warning (idempotency guard) |
| 3 failed attempts | Mark task `skipped`, needs human attention |
| PR limit reached | Exit cleanly, resume next night |
| Git pull conflict | Log error, exit — do not proceed |
| Machine crash mid-task | Next run sees no state change, retries safely |

---

## Definition of Done

**Phase 1 complete when:**
- 5 backend test tasks executed manually following instruction cards with zero improvisation
- All 5 PRs opened, CI passed, merged or closed
- Instruction cards reviewed and considered unambiguous by someone who didn't write them

**Phase 2 complete when:**
- Bot produces correct output for 10 consecutive tasks across at least two task types
- Zero human corrections needed before pushing
- Rejection rate < 10%

**Phase 3 complete when:**
- Two weeks of nightly runs without a bad PR reaching review
- Morning review takes < 15 minutes
- Team considers the process routine

**Refactoring unlocked when:**
- Backend and frontend unit test coverage has meaningfully improved
- E2E happy paths are covered
- The team is confident that CI will catch regressions introduced by the bot
