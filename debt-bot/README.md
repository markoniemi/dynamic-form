# Automated Tech Debt Reduction — Phase 1 Trial

This directory contains the orchestrator and infrastructure for automated tech debt reduction. Phase 1 is a manual trial to validate the task format and instruction cards before proceeding to full automation.

## Quick Start

### Prerequisites

1. **Python 3.10+** with `anthropic` library:
   ```bash
   pip install anthropic
   ```

2. **GitHub CLI** (`gh`) authenticated:
   ```bash
   gh auth status
   ```

3. **Claude API key** in environment:
   ```bash
   export ANTHROPIC_API_KEY="sk-ant-..."
   ```

4. **Maven** and **Node/npm** working (run from project root):
   ```bash
   ./mvnw test          # Backend tests must pass
   npm test --prefix frontend  # Frontend tests must pass
   ```

### Phase 1 Workflow

1. **Start orchestrator** for one task:
   ```bash
   cd /path/to/dynamic-form
   python debt-bot/Orchestrator.py
   ```

2. **Mark a task as ready** in `Tasks.md`:
   - Find a task with `- [ ] planned`
   - Change to `- [x] planned`
   - Save and commit (or let the orchestrator detect it)

3. **Review generated code**:
   - Orchestrator shows the generated test/code
   - Approve (y), reject (n), or edit manually
   - If approved, orchestrator tests locally

4. **If tests pass**:
   - Creates branch, commits, pushes
   - Opens PR on GitHub
   - Updates Tasks.md with PR URL

5. **If tests fail**:
   - Logs error to Status.md
   - Increments attempt counter
   - Ready to retry with better task description

---

## Files

| File | Purpose |
|------|---------|
| `Tasks.md` | Task queue (human writes, bot reads/updates checkboxes) |
| `Status.md` | Append-only log of all runs (bot writes) |
| `FindingsBackendTests.md` | Gap analysis from JaCoCo (human writes) |
| `FindingsFrontendTests.md` | Gap analysis from Jest/Vitest (for Phase 2) |
| `FindingsE2eTests.md` | Gap analysis from robot files (for Phase 2) |
| `TestRunner.py` | Runs Maven/Jest tests, detects pass/fail |
| `AiClient.py` | Calls Claude API to generate code |
| `GitClient.py` | GitHub PR operations via `gh` CLI |
| `Orchestrator.py` | Main Phase 1 loop (reads tasks, generates, tests, commits) |
| `prompts/test.txt` | Prompt template for test generation |
| `prompts/refactor.txt` | Prompt template for refactoring |
| `raw/` | Analysis output (JaCoCo, Jest, routes/endpoints — not committed) |
| `logs/` | Daily run logs (not committed) |

---

## Task Format (Tasks.md)

Each task is a markdown block:

```markdown
## TASK-NNN · [type] Short description
> Context: why this matters, which analysis signal flagged it
> File: src/path/to/NewTestFile.java  (for test tasks: file to create)
> Source file: src/path/to/Subject.java  (for test tasks: file being tested)
> Test command: ./mvnw test -Dtest=SubjectTest
> Style reference: src/test/java/.../ReferenceTest.java  (match this pattern)

Description of what to test (happy path, error path, edge cases)

- [x] planned
- [ ] branch created · `debt/task-NNN-slug`
- [ ] pr created · <url>
- [ ] ci passed
- [ ] merged
- [ ] rejected
<!-- attempts: 0 | last_error: none -->
```

**Rules:**
- `planned` = human marks task as ready to execute (human only)
- Other checkboxes = bot updates
- Only the bot may tick `branch created`, `pr created`, `ci passed`, `merged`, `rejected`
- `merged` and `rejected` are mutually exclusive
- HTML comment tracks attempt count and error

---

## Example: Manual Execution of TASK-001

### Step 1: Mark Task Ready

Edit `Tasks.md`, find TASK-001, change:
```markdown
- [ ] planned
```
to:
```markdown
- [x] planned
```

### Step 2: Run Orchestrator

```bash
cd /path/to/dynamic-form
export ANTHROPIC_API_KEY="sk-ant-..."
python debt-bot/Orchestrator.py
```

Output:
```
[Phase 1 Orchestrator] Starting manual trial
Found pending task: TASK-001

[Orchestrator] Running TASK-001: Add unit tests for FormService.getForms()
[Claude] Generating backend-test code...

================================================================================
GENERATED CODE FOR TASK-001
================================================================================
package com.example.backend.service;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
...
[complete test file]
================================================================================

Approve? (y/n/edit): y

✓ Wrote backend/src/test/java/com/example/backend/service/FormServiceTest.java

[Tests] Running: ./mvnw test -Dtest=FormServiceTest
✓ Tests passed

[Git] Creating branch: debt/task-001-add-unit-tests-for
✓ Created branch: debt/task-001-add-unit-tests-for
✓ Committed and pushed
✓ PR opened: https://github.com/markoniemi/dynamic-form/pull/42
```

### Step 3: Merge PR

Go to GitHub, merge the PR. The bot will detect it next run.

---

## Troubleshooting

### `ANTHROPIC_API_KEY not set`
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
python debt-bot/Orchestrator.py
```

### `gh CLI not authenticated`
```bash
gh auth login
```

### Tests fail during generation
1. Check `Status.md` for error logs
2. Improve the task description in `Tasks.md`
3. Increment attempt count (orchestrator does this)
4. Retry next run

### No pending tasks found
1. Open `Tasks.md`
2. Find a task with `- [ ] planned`
3. Change to `- [x] planned`
4. Run orchestrator again

---

## Next: Phase 2

After 5 tasks complete successfully with zero human corrections:

1. **Expand tasks** to frontend and e2e tests
2. **Track rejection rate** in Status.md
3. **Improve prompts** based on rejections
4. **Target**: 10 consecutive tasks with 100% acceptance

Once Phase 2 is stable, move to Phase 3 (fully automated nightly runs).

---

## References

- Full plan: `docs/PLAN.md`
- Project README: `../README.md`
