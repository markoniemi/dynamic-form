---
id: add-scope-limits-to-root-claude-md
status: done
priority: high
---

Finding C-7 of `docs/ContextAuditFindings.md`. `.claude/CLAUDE.md` never says to
stay in the module named in the task, run only that module's tests, or ask before
cross-module and parent-pom changes — a common cause of repo-wide exploration.
Same change: move "do not commit unless asked" to a deny rule, and narrow the
"prefer IntelliJ MCP tools" line so reads and search stay on
`Read`/`Grep`/`Glob`.

# Implementation Plan

Three parts, and the commit one does not work the way the finding assumed.

**1. The commit rule is `ask`, not `deny`.** A `deny` entry in Claude Code is
absolute: it blocks the command even when the human explicitly asks for a commit,
which would break `/task:init` and this very task protocol, both of which commit
by design. `ask` forces a confirmation instead, which is what "do not commit
unless asked" actually means.

The gap is real and worth closing: `.claude/settings.local.json:14` allows
`PowerShell(git *)`, which auto-approves `git commit` with no prompt at all. That
is why every commit in this session went through silently.

The prose line stays as well as the rule, shortened. Two reasons: permission
precedence between `ask` and an existing `allow` was not verified here, so prose
is the fallback if the rule turns out not to outrank `PowerShell(git *)`; and the
line costs about ten tokens.

**2. The IntelliJ line is too broad.** `mcp__idea__read_file` takes no line
range, so "prefer IntelliJ over generic file and search tools" pushes toward
whole-file reads where `Read` would take a slice. Narrow it to what IntelliJ
genuinely does better — problems, linting, formatting, building, renaming — and
leave reading and searching to `Read`/`Grep`/`Glob`.

**3. The scope limits are new.** One bullet list, naming the module boundary, the
test boundary, and what needs asking first.

Steps:

1. `.claude/CLAUDE.md` — narrow the IntelliJ bullet; shorten the commit bullet;
   add a `## Scope` section.
2. `.claude/settings.json` — add an `ask` array with `PowerShell(git commit*)`
   and `Bash(git commit*)`.
3. Verify the JSON parses and the file stays short.

No unit tests: instruction prose and one permission entry.

# Outcome

Done, two files changed, both in the repo.

`.claude/CLAUDE.md` — new `## Scope` section (module boundary, test boundary, and
what to ask about first); IntelliJ bullet narrowed to problems, lint, format,
build and rename with reads and search left to `Read`/`Grep`/`Glob`; commit
bullet shortened and now points at the enforcement.

`.claude/settings.json` — new `ask` array holding `PowerShell(git commit*)` and
`Bash(git commit*)`. Verified: the file parses, `ask` reads back correctly, and
the allow and deny lists are unchanged at 14 and 6 entries.

**This file got bigger, not smaller.** 16 lines and ~392 tokens before, 26 lines
and ~505 tokens after — the audit predicted "about one line" and this is ten. The
first draft was worse (2127 chars) and two over-long bullets were tightened to get
to 2020. The trade is deliberate: ~110 extra tokens on every session start against
one avoided cross-module exploration, which costs far more than that. But it is a
startup cost increase and should be read as one, not as a saving.

Two things not verified:

* Whether `ask` outranks the `PowerShell(git *)` allow in
  `.claude/settings.local.json:14`. If it does not, commits keep going through
  silently and that allow entry needs narrowing — which is the
  `clean settings local allowlist` task's territory.
* Whether the `Scope` section changes behavior. The only real check is watching
  how many files outside the named module get opened on the next ordinary task.
