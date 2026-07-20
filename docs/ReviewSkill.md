# ReviewSkill — Codebase Analysis and Task Card Generation

A reusable skill for analysing a codebase, identifying missing tests and
refactoring opportunities, and producing ready-to-execute task cards for the
automated debt reduction pipeline described in Plan.md.

The skill has two modes. Run them in order — always review findings before
generating task cards.

---

## Mode 1 — Findings Generation

Produces one or more `Findings*.md` files from raw analysis inputs.
Run this mode whenever starting a new analysis cycle (monthly, or after a
major release).

### Step 1 — Collect raw inputs

Run the appropriate commands for each layer you want to analyse.

**Backend test gaps (JaCoCo):**
```bash
./mvnw test jacoco:report
cp target/site/jacoco/jacoco.xml debt-bot/raw/jacoco.xml
```

**Frontend test gaps (Jest/Vitest):**
```bash
npm test -- --coverage --coverageReporters=json-summary
cp coverage/coverage-summary.json debt-bot/raw/CoverageSummary.json
```

**E2E test gaps (Robot Framework):**
```bash
grep -r "path:" src/router > debt-bot/raw/Routes.txt
grep -r "@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping" src/main \
  > debt-bot/raw/Endpoints.txt
find tests/robot -name "*.robot" -exec echo {} \; > debt-bot/raw/RobotTests.txt
```

**Refactoring gaps (run only after test generation is stable):**
```bash
git log --format=format: --name-only | sort | uniq -c | sort -rg \
  > debt-bot/raw/Churn.txt
eslint . --format json > debt-bot/raw/lint.json
./mvnw checkstyle:check > debt-bot/raw/checkstyle.txt
npx complexity-report --format json src/ > debt-bot/raw/complexity.json
```

---

### Step 2 — Run the findings prompt

Paste the collected raw files and the relevant prompt below into your AI.
One prompt per findings file. Do not mix layers in one prompt.

---

#### Prompt A — FindingsBackendTests.md

```
You are a code review assistant analysing test coverage for a Spring Boot backend.

I will give you a JaCoCo XML coverage report.

Your job:
1. List all service-layer methods with 0% line coverage.
2. List all REST controller endpoints with 0% line coverage.
3. List methods that have covered lines but uncovered exception/error branches.
4. For each finding, score IMPACT (high/medium/low) based on how critical this
   code path is to the application's correctness.
5. Risk is always LOW for test tasks — adding a new test file cannot break
   existing behaviour.
6. Produce a findings table sorted by impact descending.
7. Mark each row with a recommendation:
   ✅ automate   — low risk, any impact
   👁 human      — medium risk or requires deep domain knowledge
   ❌ skip        — generated code, migration files, config-only classes

Output format — a markdown table followed by a notes section:

| # | Class | Method | Coverage | Impact | Recommendation |
|---|---|---|---|---|---|

## Notes
- Any patterns or themes across the findings
- Classes that should be excluded from automation (generated, config, etc.)

INPUT:
[paste jacoco.xml here]
```

---

#### Prompt B — FindingsFrontendTests.md

```
You are a code review assistant analysing test coverage for a React frontend.

I will give you a Jest/Vitest coverage summary JSON.

Your job:
1. List all components with 0% statement coverage (never tested at all).
2. List components with low branch coverage (below 50%) — these have untested
   user interaction paths.
3. For each finding, describe in one sentence what user interactions are untested.
4. Score IMPACT (high/medium/low):
   - high: component handles user input, form submission, auth, or payment
   - medium: component is shared/reused across multiple pages
   - low: presentational-only component
5. Risk is always LOW — new test files cannot break existing behaviour.
6. Produce a findings table sorted by impact descending.
7. Mark each row: ✅ automate / 👁 human / ❌ skip

Output format:

| # | Component | Gap | Untested interactions | Impact | Recommendation |
|---|---|---|---|---|---|

## Notes
- Any patterns (e.g. entire feature area has no tests)

INPUT:
[paste CoverageSummary.json here]
```

---

#### Prompt C — FindingsE2eTests.md

```
You are a code review assistant identifying missing end-to-end test coverage
for a React + Spring Boot application using Robot Framework.

I will give you:
1. A list of frontend routes
2. A list of backend REST endpoints
3. A list of existing Robot Framework test files

Your job:
1. Map frontend routes to logical user journeys (e.g. /login → login flow).
2. For each user journey identify: happy path, main error path, key edge cases.
3. Check the Robot test file list for existing coverage of each journey.
4. List journeys and endpoints with no E2E coverage.
5. Score IMPACT (high/medium/low):
   - high: auth, payment, data submission, critical workflows
   - medium: standard CRUD, navigation, search
   - low: static pages, help content, minor UI states
6. Score RISK (low/medium/high):
   - low: read-only journey, no state changes
   - medium: creates or updates data
   - high: touches payments, auth tokens, or irreversible actions
7. Produce a findings table sorted by impact descending.
8. Mark each row: ✅ automate / 👁 human / ❌ skip

Output format:

| # | Journey | Missing coverage | Impact | Risk | Recommendation |
|---|---|---|---|---|---|

## Notes
- Journeys that are too complex for automation (multi-step, third-party, etc.)

INPUTS:
[paste Routes.txt]
[paste Endpoints.txt]
[paste RobotTests.txt — file paths only, not file contents]
```

---

#### Prompt D — FindingsRefactoring.md

```
You are a code review assistant identifying refactoring opportunities.

I will give you:
1. Git churn report (commit count per file)
2. Linter output (ESLint or Checkstyle)
3. Complexity report

Your job:
1. Correlate findings across all three inputs.
2. Score IMPACT (high/medium/low):
   - high: high churn + high complexity + linter findings
   - medium: two of the three signals present
   - low: single minor linter finding only
3. Score RISK (low/medium/high):
   - low: single file, no public interface, good test coverage
   - medium: touches shared utilities or multiple call sites
   - high: public API, DB access, low test coverage, high churn
4. Only include tasks that touch a single file.
5. Exclude: generated files, migrations, config files, build scripts.
6. Produce a findings table sorted by impact descending.
7. Mark each row: ✅ automate / 👁 human / ❌ skip

Output format:

| # | File | Finding | Churn | Complexity | Impact | Risk | Recommendation |
|---|---|---|---|---|---|---|---|

## Notes
- Files that appear in multiple findings (highest priority for human attention)
- Any findings that require multi-file changes (not suitable for automation)

INPUTS:
[paste Churn.txt]
[paste lint.json or checkstyle.txt]
[paste complexity.json]
```

---

### Step 3 — Review and save

Read the findings table before proceeding. Check:
- Does the recommendation column look reasonable?
- Are any critical areas missing? (AI can miss things not visible in coverage reports)
- Are any rows marked `✅ automate` that a human would consider risky?

Correct anything that looks wrong, then save the file to `debt-bot/`.

---

## Mode 2 — Task Card Generation

Produces ready-to-paste task cards for `Tasks.md`.
Run this mode after findings have been reviewed and approved.

### Step 1 — Select candidates

From the findings table, pick rows marked `✅ automate`. Work top-down by impact.
Select enough for one week of nightly runs (typically 10–20 tasks).

Skip a row if:
- You cannot name the exact file the bot should create or edit
- You cannot write the exact test command
- You cannot find a good style reference file in the existing codebase

### Step 2 — Run the task card prompt

One prompt call per task. Paste the relevant finding row and supporting context.

---

#### Prompt E — Backend or Frontend Test Card

```
You are a technical writer producing an instruction card for an automated
test-writing bot. The bot has no context beyond what you write here.
It will follow the instructions literally with no improvisation.

FINDING:
[paste the row from FindingsBackendTests.md or FindingsFrontendTests.md]

STYLE REFERENCE FILE PATH: [e.g. src/test/java/.../OrderServiceTest.java]
STYLE REFERENCE CONTENT:
[paste the content of the style reference file]

Produce a task card in exactly this format:

## TASK-NNN · [backend-test|frontend-test] <short description>
> Context: <one sentence — why this matters, which signal flagged it>
> File: <exact path of the NEW test file to create>
> Source file: <exact path of the file being tested>
> Test command: <exact command to run only this test>
> Style reference: <path of the style reference file>

### Scenarios to test
1. <happy path — specific inputs and expected output>
2. <error path — specific error condition and expected behaviour>
3. <edge case — boundary value or null input>
[add more if the finding warrants it]

### Do not
- Do not test anything not listed above
- Do not modify any existing files
- Do not add utility methods or helpers not needed for these scenarios

- [x] planned
- [ ] branch created · `debt/task-NNN-<slug>`
- [ ] pr created · <url>
- [ ] ci passed
- [ ] merged
- [ ] rejected
<!-- attempts: 0 | last_error: none -->

Rules for the card:
- Every scenario must specify exact inputs and expected outputs, not vague goals
- The test command must run only this one test file, not the whole suite
- If you cannot write a specific scenario without knowing business logic,
  write [HUMAN: describe the expected behaviour here] as a placeholder
  and flag the card as needing human completion before use
```

---

#### Prompt F — Robot E2E Test Card

```
You are a technical writer producing an instruction card for an automated
Robot Framework test-writing bot. The bot has no context beyond what you
write here. It must be able to write the test file without any judgment calls.

FINDING:
[paste the row from FindingsE2eTests.md]

STYLE REFERENCE FILE PATH: [e.g. tests/robot/auth/LoginSuccess.robot]
STYLE REFERENCE CONTENT:
[paste the content of the style reference file]

APPLICATION CONTEXT:
- Frontend URL: [e.g. http://localhost:3000]
- Key page URLs: [list relevant routes]
- Any relevant API endpoints: [list if known]

Produce a task card in exactly this format:

## TASK-NNN · [e2e] <short description>
> Context: <one sentence — why this journey needs coverage>
> File: <exact path of the NEW .robot file to create>
> Test command: robot --suite <SuiteName> <path>
> Style reference: <path of the style reference file>

### Journey steps
1. <Navigate to / open page>
2. <Perform action — be specific: click button labelled X, enter value Y>
3. <Assert outcome — be specific: page shows element Z, URL changes to W>
[continue until journey is complete]

### Assertions
- <list each thing that must be true at the end of the journey>

### Do not
- Do not test anything beyond this journey
- Do not create shared keywords or resource files
- Do not modify any existing files

- [x] planned
- [ ] branch created · `debt/task-NNN-<slug>`
- [ ] pr created · <url>
- [ ] ci passed
- [ ] merged
- [ ] rejected
<!-- attempts: 0 | last_error: none -->

Rules for the card:
- Every step must be specific enough that a bot can execute it without
  knowing the application — include exact labels, field names, button text
- If exact labels are unknown, write [HUMAN: verify button label] as a
  placeholder and flag the card as needing human completion before use
```

---

#### Prompt G — Refactoring Card

```
You are a technical writer producing an instruction card for an automated
refactoring bot. The bot has no context beyond what you write here.
It will follow the instructions literally with no improvisation.

FINDING:
[paste the row from FindingsRefactoring.md]

CURRENT FILE CONTENT:
[paste the content of the file to be changed]

Produce a task card in exactly this format:

## TASK-NNN · [refactor] <short description>
> Context: <one sentence — why this matters, which signal flagged it>
> File: <exact path of the file to edit>
> Test command: <exact command to verify nothing is broken>

### Instructions
1. <specific change — line numbers or exact strings where possible>
2. <next change>
[keep steps atomic — one logical change per step]

### Do not
- Do not change any logic
- Do not reformat unrelated code
- Do not add or remove comments unrelated to the change

- [x] planned
- [ ] branch created · `debt/task-NNN-<slug>`
- [ ] pr created · <url>
- [ ] ci passed
- [ ] merged
- [ ] rejected
<!-- attempts: 0 | last_error: none -->

Rules for the card:
- Instructions must be executable without judgment — if a step requires
  understanding business logic, the task is not suitable for automation
- If the change touches more than one file, split into separate cards or
  mark as 👁 human
```

---

### Step 3 — Quality check each card

Before adding to `Tasks.md`, verify against this checklist:

| Check | Good | Bad |
|---|---|---|
| Scope | Single file, <50 lines | Multiple files, architectural |
| Instructions | Step-by-step, no judgment | "Improve the error handling" |
| Test command | Runs only this file | Runs the whole suite |
| File path | Explicit, full path | Left for bot to decide |
| Scenarios | Specific inputs and outputs | Vague goals |
| Style reference | Points to a real existing file | Missing |
| Placeholders | Zero [HUMAN: ...] remaining | Any placeholder not yet filled |

A card with any `[HUMAN: ...]` placeholder must be completed by a human before
it is marked `[x] planned`. Do not pass incomplete cards to the bot.

### Step 4 — Add to Tasks.md

Append approved cards to `debt-bot/Tasks.md` in priority order.
Each card should already have `- [x] planned` ticked.
Commit `Tasks.md` to main with message: `human: add N task cards from findings`.

---

## Quick Reference — Mode Selection

| Situation | Mode |
|---|---|
| Starting fresh, no findings yet | Mode 1 — all relevant prompts |
| Findings exist, need task cards | Mode 2 only |
| Monthly refresh | Mode 1, then compare with existing Tasks.md |
| Single new area to cover | Mode 1 with the relevant prompt only |
| Findings look thin or wrong | Re-run Mode 1 with more context added to the prompt |
