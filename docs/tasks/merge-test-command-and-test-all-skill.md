---
id: merge-test-command-and-test-all-skill
status: in_review
priority: low
---

Finding C-9 of `docs/ContextAuditFindings.md`. `.claude/commands/test.md` (unit
only) and `.claude/skills/test-all/SKILL.md` (unit + integration) are
near-identical. One skill with a `--with-integration` argument.

# Implementation Plan

Merge the two into `.claude/skills/test/SKILL.md`, one section per mode, and
delete both originals. `/test` keeps working, since a skill is invocable by name.

**A problem found while reading them, which changes the merge.** Both recipes are
bash — `$(mktemp)`, `grep`, `tail` — but `~/.claude/settings.json` sets
`"deny": ["Bash"]`, and deny beats allow. Neither recipe can run on this machine;
merging them faithfully would produce a skill that fails on its first command. So
the merged skill is written in PowerShell, which is what this environment
actually has (`CLAUDE_CODE_USE_POWERSHELL_TOOL=1`, `defaultShell: powershell`).

Carried over from the gated Stop hook: `"-Dskip.npm"` stays quoted, because
PowerShell splits it at the dot and Maven then reads `.npm` as a lifecycle phase.

The two modes keep their real difference, which is not just a flag:

* unit-only passes `-Dskip.npm` and runs Vitest separately
* `--with-integration` runs `mvn -pl backend -am verify`, whose `-am` build runs
  the frontend's `npm run ci` and so includes Vitest already

# Outcome

Done. `.claude/skills/test/SKILL.md` created; `.claude/commands/test.md` and
`.claude/skills/test-all/SKILL.md` removed with `git rm`. The freshness check on
`surefire-reports` and `failsafe-reports`, and the "trust the exit code, not the
filtered text" rule, are kept from the originals.

**Verified by running it.** The commands are translations rather than copies, so
the default mode was executed end to end:

```
Test Files  8 passed (8)
Tests  52 passed (52)
[INFO] Tests run: 73, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
vitest exit: 0, mvn exit: 0
```

The filtered output included two `ERROR`/`WARN` lines from
`GlobalExceptionHandlerTest`, which exercises error paths on purpose. They look
alarming and mean nothing — which is exactly why the skill keeps the originals'
"trust the exit codes, not the filtered text" rule.

Not run: the `--with-integration` mode. It needs Docker and a jib image build,
and the default mode shares all of its reporting logic.
