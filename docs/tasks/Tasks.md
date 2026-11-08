---
create_branch: false
create_pull_request: false
---

# Task Backlog

Inbox for new tasks. A human adds an entry; the AI turns it into a task file. See
`README.md` for how. The rules come from the `task` plugin, not from this
directory.

The frontmatter above is this project's configuration. `create_branch` decides
whether a task may have its own branch; `create_pull_request` decides whether
reaching `approved` opens a pull request on GitHub, or a merge request on GitLab.
Each key is a ceiling: `false` takes the thing away from every task, whatever else
is written down. Edit them here and the next task follows them.

Entry format: `### <short title> [<status>, <priority>]` plus a short explanation.
Once the AI has created the task file, it adds a `plan: <link>` line under the
heading and keeps the brackets up to date as the status changes. An entry is
removed when its task is cleaned, so this file shows open work only.

Keep a heading short and plain: it becomes the task file's name, and the branch
name when the project uses branches.

Statuses:

* `planned` - written down, not planned yet.
* `planning` - a human asked the AI to plan it.
* `planned` - plan written, waiting for a human to verify it.
* `in_progress` - the AI is implementing it.
* `approved` - the implementation is ready, waiting for review.
* `changes_requested` - review asked for changes.
* `approved` - review passed, ready to land.
* `done` - merged, or committed on `main` when the project uses no branches.

Priorities: `high` | `medium` | `low`.

---

## Backlog

Entries below come from `docs/ContextAuditFindings.md` (context/token audit,
2026-11-07). Each names the finding that explains it.

### remove duplicate caveman hooks [approved, high]

plan: [remove-duplicate-caveman-hooks.md](remove-duplicate-caveman-hooks.md)

Finding C-4. The caveman mode text is injected twice per session and twice per
prompt: `~/.claude/settings.json` runs `caveman-activate.js` and
`caveman-mode-tracker.js` while the `caveman@caveman` plugin registers the same
hooks. Remove the two manual entries from user settings, keep the plugin's.
Outside the repo — user-level config.

### fix stale skill facts [approved, high]

plan: [fix-stale-skill-facts.md](fix-stale-skill-facts.md)

Finding C-1. `dynamic-form-backend/SKILL.md` claims Spring Boot 3.5.6, Java 17+,
H2 for dev and a `dto/` package; actual is 4.0.3, Java 21, Postgres for dev, DTOs
in `api`. It also tells the agent to `cd backend; mvn test`, which
`.claude/commands/test.md` explicitly forbids. Two wrong paths as well:
`config/GlobalExceptionHandler.java` in the fullstack skill and
`src/context/oidcConfig.tsx` in the frontend skill.

### dedupe mcp browser servers [approved, high]

plan: [dedupe-mcp-browser-servers.md](dedupe-mcp-browser-servers.md)

Finding C-3. Playwright is configured twice — repo `.mcp.json` and the
`playwright@claude-plugins-official` plugin — so both tool sets load at startup;
`claude-in-chrome` is a third browser stack. Keep the repo-pinned server, disable
the plugin, and drop the six redundant per-tool allow entries at
`.claude/settings.json:16-21`.

### trim generic advice from skills [approved, high]

plan: [trim-generic-advice-from-skills.md](trim-generic-advice-from-skills.md)

Finding C-2. `dynamic-form-backend/SKILL.md:88-142` and
`dynamic-form-frontend/SKILL.md:67-132` recite Effective Java, Clean Code,
Effective TypeScript and the React docs — about half of each body, and nothing
project-specific. Keep only the rules where this project deviates; move or drop
the rest.

### prune unused plugins [approved, high]

plan: [prune-unused-plugins.md](prune-unused-plugins.md)

Finding C-5. 7 plugins, 34 skills and 9 agents are paid at startup, with five
overlapping review paths and three task/plan systems. Read `/usage` first to see
which are actually used, then disable the rest. Outside the repo — user-level
config.

### add scope limits to root claude md [approved, high]

plan: [add-scope-limits-to-root-claude-md.md](add-scope-limits-to-root-claude-md.md)

Finding C-7. `.claude/CLAUDE.md` never says to stay in the module named in the
task, run only that module's tests, or ask before cross-module and parent-pom
changes — a common cause of repo-wide exploration. Same change: move "do not
commit unless asked" to a deny rule, and narrow the "prefer IntelliJ MCP tools"
line so reads and search stay on `Read`/`Grep`/`Glob`.

### dedupe error table and project maps [approved, medium]

plan: [dedupe-error-table-and-project-maps.md](dedupe-error-table-and-project-maps.md)

Finding C-8. The exception → HTTP table appears three times across the backend and
fullstack skills, and the skills repeat project maps that the module `CLAUDE.md`
files already own. Single-source each; C-1 is this duplication already failing.

### gate stop hook on changed paths [approved, medium]

plan: [gate-stop-hook-on-changed-paths.md](gate-stop-hook-on-changed-paths.md)

Finding C-10. The Stop hook in `.claude/settings.json:45` runs frontend lint, tsc
and `mvn compile` after every turn, including read-only ones. Gate it on
`git diff --name-only`, and fix the `cmd` `&` chaining that swallows a `tsc`
failure.

### merge test command and test-all skill [approved, low]

plan: [merge-test-command-and-test-all-skill.md](merge-test-command-and-test-all-skill.md)

Finding C-9. `.claude/commands/test.md` (unit only) and
`.claude/skills/test-all/SKILL.md` (unit + integration) are near-identical. One
skill with a `--with-integration` argument.

### clean settings local allowlist [approved, low]

plan: [clean-settings-local-allowlist.md](clean-settings-local-allowlist.md)

Finding C-12. `.claude/settings.local.json:23-28` holds six allow entries that are
whole one-off commands and will never match again. Replace with the broad
`PowerShell(...)` patterns, or run `/fewer-permission-prompts`.

### review ponytail codebase map [approved, low]

plan: [review-ponytail-codebase-map.md](review-ponytail-codebase-map.md)

Finding C-6. The ponytail SessionStart hook injects a codebase map every session
(~280 tok) that overlaps the Layout sections in each module `CLAUDE.md` and goes
stale silently. Decide whether it earns its place. Outside the repo — user-level
config.

### commit deny rule [approved, low]

plan: [commit-deny-rule.md](commit-deny-rule.md)

- remove commit deny rule, no need to ask for permission to commit files.

### ponytail [done, low]

file: [ponytail.md](ponytail.md)

- run ponytail audit, write findings to a file. do not act on findings yet.

### dependency update [approved, medium]

plan: [dependency-update.md](dependency-update.md)

- update maven and npm dependencies, use context7.

### update playwright [in_review, medium]

file: [update-playwright.md](update-playwright.md)

Split out of `dependency update`, which reverted it. Playwright (Java) 1.48.0 →
1.64.0 makes `FrontendIT.loginAndFormSubmission` (`:66`) and
`FrontendIT.editSubmission` (`:102`) fail on their final
`submissionDetailPage.containsText(...)` assertion, reproducibly, while the same
verify is green on 1.48.0. Find out why and bump, or record that we stay on
1.48.0. Start at `SubmissionDetailPage.containsText`, which calls `textContent()`
on the details card. Note 1.64 downloads new browsers on its first run — that is
what made `AdminRoleIT` take 138 s, not a test failure.
- perhaps test 1.61.0 first.

### eslint-rules [approved, medium]

file: [eslint-rules.md](eslint-rules.md)

- Add no-misused-promises, no-floating-promises and no-base-to-string to eslint.

### waitForCondition [in_progress, low]

file: [waitforcondition.md](waitforcondition.md)

- remove BasePage.sleep
