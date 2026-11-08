---
id: clean-settings-local-allowlist
status: done
priority: low
---

Finding C-12 of `docs/ContextAuditFindings.md`.
`.claude/settings.local.json:23-28` holds six allow entries that are whole
one-off commands and will never match again. Replace with the broad
`PowerShell(...)` patterns, or run `/fewer-permission-prompts`.

# Implementation Plan

The file had 20 entries. Eight were junk auto-added from past approvals: four
whole `PowerShell(...)` pipelines with escaped braces that cannot match a second
time, plus `Bash(Get-ChildItem -Recurse -Filter "*oauth*" ...)` and
`Bash(Select-Object FullName)`, which are PowerShell commands recorded under the
`Bash` tool and so doubly dead.

Also dropped: `Bash(git add *)` and `Bash(git commit -m ' *)`. The `Bash` tool is
denied outright in `~/.claude/settings.json`, so every `Bash(...)` entry here is
inert; the `gh` ones are kept only because they would start working if that deny
were ever lifted.

Kept deliberately: `PowerShell(git *)`. The `commit-deny-rule` task settled that
commits should not prompt, and narrowing this entry would undo that decision.

Added: `PowerShell(Select-String *)` and `PowerShell(Test-Path *)`, both
read-only and both already approved ad hoc during this work.

# Outcome

Done. `.claude/settings.local.json` rewritten: 20 entries down to 19, but the
eight dead ones are gone and replaced by patterns that will actually match.
`WebFetch(domain:raw.githubusercontent.com)` moved up next to the other
`WebFetch` entries rather than sitting among the shell commands.

No token saving here, and the finding said as much — permission lists are not part
of the context window. The gain is that the prompts these were meant to prevent
will now actually be prevented.
