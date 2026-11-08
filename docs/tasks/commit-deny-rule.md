---
id: commit-deny-rule
status: done
priority: low
---

- remove commit deny rule, no need to ask for permission to commit files.

# Implementation Plan

This reverses part of the `add-scope-limits-to-root-claude-md` task, which added
an `ask` array for `git commit` on the strength of finding C-7. The human's call
overrides the finding: committing should not prompt.

Taken out of priority order deliberately. It is `low`, so the backlog ordering
would leave it until last, but it makes every commit prompt in the meantime, and
it corrects a change made one tick earlier.

Steps:

1. `.claude/settings.json` — remove the `ask` array entirely. `PowerShell(git *)`
   in `.claude/settings.local.json` then governs commits again, as it did before.
2. `.claude/CLAUDE.md` — restore the original wording, "Do not commit unless
   asked", and drop the clause pointing at the enforcement that no longer exists.
   The guidance stays; only the permission prompt goes.
3. Verify the JSON parses and `allow`/`deny` are unchanged.

# Outcome

Done. The `ask` array is out of `.claude/settings.json`, and
`.claude/CLAUDE.md` says "Do not commit unless asked" again, as it did before
the scope-limits task touched it. Commits do not prompt.

Note for the related task: finding C-7 claimed an absolute rule in a context file
"is guidance Claude may override" and belongs in a permission rule. For commits on
this project that trade is now settled the other way — the prose is the whole
mechanism, by choice. The `clean settings local allowlist` task should therefore
leave `PowerShell(git *)` broad enough to keep covering commits.
