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
reaching `in_review` opens a pull request on GitHub, or a merge request on GitLab.
Each key is a ceiling: `false` takes the thing away from every task, whatever else
is written down. Edit them here and the next task follows them.

Entry format: `### [<kind>: ]<short title> [<status>, <priority>]` plus a short
explanation. Once the AI has created the task file, it adds a `file: <link>` line
under the heading and keeps the brackets up to date as the status changes. An entry
is removed when its task is cleaned, so this file shows open work only.

Keep a heading short and plain: it becomes the task file's name, and the branch
name when the project uses branches.

Statuses:

* `draft` - written down, not planned yet.
* `planning` - a human asked the AI to plan it.
* `planned` - cleared to implement. Either a plan is written and waiting for a
  human to verify it, or you set `planned` yourself **with no plan** in the task
  file, which waives the plan and tells the AI to go straight to the code.
* `in_progress` - the AI is implementing it.
* `in_review` - the implementation is ready, waiting for review.
* `changes_requested` - review asked for changes.
* `approved` - review passed, ready to land.
* `done` - merged, or committed on `main` when the project uses no branches.

Priorities: `high` | `medium` | `low`.

---

## Backlog

Entries below come from `docs/ContextAuditFindings.md` (context/token audit,
2026-11-07). Each names the finding that explains it.

### gate stop hook on changed paths [done, medium]

file: [gate-stop-hook-on-changed-paths.md](gate-stop-hook-on-changed-paths.md)

Finding C-10. The Stop hook in `.claude/settings.json:45` runs frontend lint, tsc
and `mvn compile` after every turn, including read-only ones. Gate it on
`git diff --name-only`, and fix the `cmd` `&` chaining that swallows a `tsc`
failure.

### merge test command and test-all skill [done, low]

file: [merge-test-command-and-test-all-skill.md](merge-test-command-and-test-all-skill.md)

Finding C-9. `.claude/commands/test.md` (unit only) and
`.claude/skills/test-all/SKILL.md` (unit + integration) are near-identical. One
skill with a `--with-integration` argument.

### clean settings local allowlist [done, low]

file: [clean-settings-local-allowlist.md](clean-settings-local-allowlist.md)

Finding C-12. `.claude/settings.local.json:23-28` holds six allow entries that are
whole one-off commands and will never match again. Replace with the broad
`PowerShell(...)` patterns, or run `/fewer-permission-prompts`.

### review ponytail codebase map [done, low]

file: [review-ponytail-codebase-map.md](review-ponytail-codebase-map.md)

Finding C-6. The ponytail SessionStart hook injects a codebase map every session
(~280 tok) that overlaps the Layout sections in each module `CLAUDE.md` and goes
stale silently. Decide whether it earns its place. Outside the repo — user-level
config.

### commit deny rule [done, low]

file: [commit-deny-rule.md](commit-deny-rule.md)

- remove commit deny rule, no need to ask for permission to commit files.

### ponytail [done, low]

file: [ponytail.md](ponytail.md)

- run ponytail audit, write findings to a file. do not act on findings yet.

### dependency update [done, medium]

file: [dependency-update.md](dependency-update.md)

- update maven and npm dependencies, use context7.

### update playwright [done, medium]

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

### eslint-rules [done, medium]

file: [eslint-rules.md](eslint-rules.md)

- Add no-misused-promises, no-floating-promises and no-base-to-string to eslint.

### waitForCondition [done, low]

file: [waitforcondition.md](waitforcondition.md)

- remove BasePage.sleep

### lombok [planned, medium]

file: [lombok.md](lombok.md)

- lombok is not used in all places where it could. Check where lombok could be used.

### shorten test methods [done, medium]

file: [shorten-test-methods.md](shorten-test-methods.md)

Merged in `FindBy annotation` ("Selenium has FindBy annotation, using it makes easy
to act on page elements. Does playwright have something similar?") — both rewrite
the same five page objects.

- FormSubmissionPage.fillTextArea could be written as
```java
  public void fillTextArea(String fieldName, String value) {
  page.fill("textarea[name='%s']".formatted(fieldName), value);
}
```
- is waitForSelector required? looks like overkill to wait for selector in every method. loginAndFormSubmission test calls formSubmissionPage.waitForLoad(); before setting input values.

### Migrate selectors [done, medium]

file: [migrate-selectors.md](migrate-selectors.md)

Migrating the selectors from CSS/XPath to getByRole/getByLabel/getByTestId. That is what Playwright actually recommends, but it rewrites every selector against the real DOM, so it is a different risk from a mechanical field swap. Own backlog entry if wanted.
