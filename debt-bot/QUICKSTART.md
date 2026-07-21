# Phase 1 Quick Start — 5 Minutes

## Setup (first time only)

```bash
# 1. Install Python dependencies
pip install -r debt-bot/requirements.txt

# 2. Authenticate with GitHub CLI
gh auth status
# If not authenticated, run: gh auth login

# 3. Set Claude API key (in PowerShell on Windows)
$env:ANTHROPIC_API_KEY = "sk-ant-..."
```

## Run First Task

```bash
# 1. Check what tasks are ready
cat debt-bot/Tasks.md | grep "planned"

# 2. Mark TASK-001 as planned (edit Tasks.md, change [ ] to [x])
# Look for: - [ ] planned
# Change to: - [x] planned

# 3. Run the orchestrator
python debt-bot/Orchestrator.py

# 4. Follow the prompts:
#    - View generated test code
#    - Type 'y' to approve, 'n' to reject, 'edit' to modify manually
#    - Orchestrator will test, commit, and push

# 5. Check GitHub for the PR
gh pr list --repo markoNiemi/dynamic-form
```

## What Happens

1. **Orchestrator finds TASK-001** (first task with [x] planned)
2. **Reads source file** and style reference
3. **Calls Claude API** to generate a test file
4. **Shows you the code** — review and approve
5. **Runs local tests** — `./mvnw test -Dtest=FormServiceTest`
6. **If tests pass**:
   - Creates branch `debt/task-001-...`
   - Commits and pushes
   - Opens PR on GitHub
   - Updates Tasks.md with PR URL
7. **If tests fail**:
   - Logs error to Status.md
   - Increments attempt count
   - Ready to retry with better task description

## Success Looks Like

```
[Orchestrator] Running TASK-001: Add unit tests for FormService.getForms()
[Claude] Generating backend-test code...
✓ Tests passed
✓ PR opened: https://github.com/markoNiemi/dynamic-form/pull/42
```

## Next Task

Edit Tasks.md again, mark next task as `[x] planned`, and run:
```bash
python debt-bot/Orchestrator.py
```

Target: **5 tasks completed**, all with green CI, all merged or reviewed.

---

## Help

- **Tests fail locally?** → Check `Status.md` for error logs, improve task description, retry
- **No pending tasks?** → Open `Tasks.md`, mark a task as `[x] planned`
- **PR not created?** → Run `gh auth status` to check authentication
- **API key issue?** → `echo $env:ANTHROPIC_API_KEY` (should show `sk-ant-...`)

See `README.md` for detailed docs.
