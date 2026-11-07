---
id: remove-duplicate-caveman-hooks
status: in_progress
priority: high
---

Finding C-4 of `docs/ContextAuditFindings.md`. The caveman mode text is injected
twice per session and twice per prompt: `~/.claude/settings.json` runs
`caveman-activate.js` and `caveman-mode-tracker.js` while the `caveman@caveman`
plugin registers the same hooks. Remove the two manual entries from user
settings, keep the plugin's. Outside the repo — user-level config.

# Implementation Plan

Verified before changing anything:

* `~/.claude/plugins/cache/caveman/caveman/ef6050c5e184/.claude-plugin/plugin.json`
  registers both hooks itself — `SessionStart` → `caveman-activate.js`,
  `UserPromptSubmit` → `caveman-mode-tracker.js` — under
  `${CLAUDE_PLUGIN_ROOT}/hooks/`. So the mode survives removing the manual pair.
* Every caveman script resolves its state directory as
  `CLAUDE_CONFIG_DIR || ~/.claude`, never relative to the script. The plugin's
  copies therefore read and write the same state the manual ones did, and
  `caveman-statusline.ps1` keeps working.
* The `statusLine` entry is **not** duplicated by the plugin. It stays, and so do
  the script files in `~/.claude/hooks/` that it needs.

Steps:

1. Back up `~/.claude/settings.json` (not version controlled) to the session
   scratchpad.
2. Delete the whole `"hooks"` block from `~/.claude/settings.json` — both entries
   in it are the duplicates. Leave `statusLine`, `skillOverrides`,
   `enabledPlugins` and the rest untouched.
3. Leave `~/.claude/hooks/*.js` in place: the plugin does not use them, but
   deleting files outside the repo is not this task.
4. Verify the JSON still parses.

No unit tests: the change deletes configuration and adds no logic. Verification
is the next session start — one caveman block instead of two, one per-prompt line
instead of two.
