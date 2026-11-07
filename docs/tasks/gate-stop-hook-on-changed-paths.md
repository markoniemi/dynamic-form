---
id: gate-stop-hook-on-changed-paths
status: in_review
priority: medium
---

Finding C-10 of `docs/ContextAuditFindings.md`. The Stop hook in
`.claude/settings.json:45` runs frontend lint, tsc and `mvn compile` after every
turn, including read-only ones. Gate it on `git diff --name-only`, and fix the
`cmd` `&` chaining that swallows a `tsc` failure.

# Implementation Plan

Three defects in the current one-liner, not two:

1. It runs on every Stop, including documentation, audit and read-only turns.
2. `cmd` chains with `&`, which runs each command regardless of the previous
   one's result and yields only the **last** command's exit code — so a `tsc`
   failure is reported as success as long as `mvn` passes.
3. It uses `cd ../backend && mvn compile`, the very pattern
   `.claude/commands/test.md` warns about. It should be `mvn -pl backend`.

**Why a script and not a longer one-liner.** The gate needs to read `git status`,
match path prefixes and keep two exit codes apart. In a `cmd /c` string that is
unreadable and unmaintainable; as `.claude/hooks/stop-check.ps1` it is plain code
that can be run by hand.

**Which paths count as "touched".** Not just the working tree: this task protocol
commits as it goes, so a turn's own changes are often already in `HEAD` and
`git status` is clean. The script therefore unions the working tree with the last
commit. A docs-only turn still matches neither module and skips.

**Fallback.** If git cannot be read at all, the script checks everything, so a
broken gate never silently skips a real compile error.

**Self-check.** The script takes `-DryRun`, which prints the decision without
running a build. That makes both branches testable in a second instead of a
two-minute Maven run, and it is how this change gets verified.

Steps:

1. Write `.claude/hooks/stop-check.ps1`.
2. Verify with `-DryRun` that a `.claude`-only change skips both checks, and that
   frontend and backend paths each select the right one.
3. Point the Stop hook at the script in `.claude/settings.json`.

No unit tests beyond the dry-run self-check: the script's only logic is path
matching and exit-code handling, and `-DryRun` exercises the former directly.

# Outcome

Done. New file `.claude/hooks/stop-check.ps1`; the Stop hook in
`.claude/settings.json` now calls it instead of the `cmd /c` one-liner. All three
original defects are fixed: the gate skips untouched modules, `tsc` and `mvn`
failures are collected separately so neither masks the other, and the Maven call
is `-pl backend -am` rather than `cd ../backend`.

Verified, in this order:

| Check | Result |
| :- | :- |
| dry run, only `.claude` and `docs` touched | both checks `False` — skips |
| dry run, probe file in `frontend/` | frontend `True`, backend `False` |
| dry run, probe file in `api/` | frontend `False`, backend `True` |
| real run, nothing touched | exit 0 in 0.4s (was a full lint + tsc + compile every turn) |
| real run, probe in `api/` | backend branch ran, compiled, exit 0 in 5.1s |

Probe files were untracked and deleted immediately; `git status` confirmed clean
after each.

**A bug the verification caught.** The first version passed `-Dskip.npm`
unquoted. PowerShell splits that at the dot, so Maven received `.npm` as a
lifecycle phase and failed with `Unknown lifecycle phase ".npm"`. Had this not
been run end to end, every backend turn would have been blocked by a hook failure.
It is quoted now, with a comment saying why.

Not verified: the failure path. Confirming that a genuine `tsc` or compile error
produces exit 2 and readable output would mean deliberately breaking code, so it
is untested. The success path and the gate are both confirmed.
