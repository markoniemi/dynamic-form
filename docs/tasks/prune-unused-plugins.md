---
id: prune-unused-plugins
status: in_review
priority: high
---

Finding C-5 of `docs/ContextAuditFindings.md`. 7 plugins, 34 skills and 9 agents
are paid at startup, with five overlapping review paths and three task/plan
systems. Read `/usage` first to see which are actually used, then disable the
rest. Outside the repo — user-level config.

# Implementation Plan

`/usage` is a human's command, so the measurement came from the eight session
transcripts for this project in
`~/.claude/projects/C--Users-marko-Documents-Git-dynamic-form/*.jsonl` (12 MB).
Raw name mentions are useless — every session's startup context lists all 34
skill names — so what was counted is `"skill": "<name>"` invocations and
`<command-name>` slash commands.

| Plugin | Invocations found | Verdict |
| :- | :- | :- |
| `context@context-plugin` | `/context:audit` ×6 | keep, heavily used |
| `task@task-plugin` | `/task:init` ×2, `task:next-task` ×3 | keep, in use right now |
| `ponytail@ponytail` | `/ponytail:ponytail-audit` ×1, plus the live SessionStart mode | keep |
| `caveman@caveman` | no skill calls, but the mode is active through its hooks and the statusline | keep — the mode *is* the usage |
| `context7@claude-plugins-official` | no recorded tool call in this project | keep: 2 tools, and the audit rates its docs value above its cost |
| `superpowers@claude-plugins-official` | `superpowers:executing-plans` ×2, and that skill is now `off` in `skillOverrides` | **disable for this project** |

So superpowers is the only plugin the evidence supports cutting: 17 skill
descriptions at startup plus a full `using-superpowers` text injection every
session, four skills already explicitly `off`, and the one skill ever invoked
turned off too.

Scoping matters here. The evidence is from this project's sessions only, and
`superpowers` is enabled in **both** `~/.claude/settings.json` and the repo's
`.claude/settings.json`. Disabling it in user settings would take it away from
every other project, which this evidence says nothing about. So the change goes
in the repo's `.claude/settings.json`, where its scope matches its evidence.

Steps:

1. `.claude/settings.json` — set `superpowers@claude-plugins-official` to
   `false`. Leave `~/.claude/settings.json` alone.
2. Verify the file parses and `ponytail` is still enabled.

Left alone deliberately:

* `mcp__idea__*` and `mcp__playwright__*` — not plugins. The IntelliJ preference
  is finding C-7's business, and playwright is repo-pinned for the e2e tests.
* `claude-in-chrome` — enabled outside these files; `/mcp` is the human's.
* The `caveman:cavecrew-*` agents — they duplicate `Explore`, but they ship with a
  plugin that stays enabled, so there is nothing to switch off separately.

No unit tests: the change is one boolean in a settings file.

# Outcome

Done, one line changed: `.claude/settings.json` now has
`"superpowers@claude-plugins-official": false`. `ponytail` is still `true`, and
`~/.claude/settings.json` still has superpowers `true`, so other projects keep
it. This change **is** in the repo, unlike the previous two config tasks.

Five plugins were kept, each on evidence rather than preference — the table above
records what was found for each.

**Open question for the human, and the reason this is not more than one line:**
whether a project-level `false` actually overrides a user-level `true` for
`enabledPlugins` was not verified. If the next `/reload-plugins` still reports
superpowers' 17 skills, precedence runs the other way and the toggle has to go in
`~/.claude/settings.json` instead — which would disable superpowers for every
project, a wider change than this evidence supports. Worth checking the plugin
count before assuming this worked.
