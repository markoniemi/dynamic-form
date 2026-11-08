---
id: dedupe-mcp-browser-servers
status: done
priority: high
---

Finding C-3 of `docs/ContextAuditFindings.md`. Playwright is configured twice —
repo `.mcp.json` and the `playwright@claude-plugins-official` plugin — so both
tool sets load at startup; `claude-in-chrome` is a third browser stack. Keep the
repo-pinned server, disable the plugin, and drop the six redundant per-tool allow
entries at `.claude/settings.json:16-21`.

# Implementation Plan

Why the repo server is the one to keep: `.mcp.json` pins
`@playwright/mcp@0.0.83`, so everyone who clones gets the same version, and
`.claude/settings.json` already allows `mcp__playwright__*` and lists the server
in `enabledMcpjsonServers`. The plugin adds the same tools under a second prefix
(`mcp__plugin_playwright_playwright__*`) that nothing allows.

Checked: the backend `e2e/` integration tests drive Playwright through its Java
binding (`PlaywrightConfig`, page objects), not through MCP, so neither server is
a test dependency.

Steps:

1. `~/.claude/settings.json` — set `playwright@claude-plugins-official` to
   `false` in `enabledPlugins`. Set rather than delete, so the entry stays
   visible and is one edit to undo.
2. `.claude/settings.json` — delete the six `mcp__playwright__browser_*` allow
   entries. `mcp__playwright__*` on the line above already covers all of them.
3. Verify both files still parse as JSON.

`claude-in-chrome` is **not** touched. It is not configured in either file — it
is a connector enabled outside these settings, so turning it off is `/mcp`, which
is the human's to run.

No unit tests: the change deletes configuration and adds no logic. Verification
is the next session's tool list.

# Outcome

Done, two files changed:

* `~/.claude/settings.json` — `playwright@claude-plugins-official` set to
  `false`. **Outside the repo, so no commit contains it.**
* `.claude/settings.json` — the six `mcp__playwright__browser_*` entries removed;
  the allow list goes from 20 entries to 14.

Verified after the edits: both files parse as JSON; the plugin reads `false`; the
other five plugins are still `true`; the only playwright allow entry left is the
`mcp__playwright__*` wildcard; `enabledMcpjsonServers` still lists `playwright`;
the `Stop` hook is still there.

Not verified: that only one playwright tool set loads. A plugin toggle applies at
the next session start, so this session still has both. The next session's tool
list is the check.

`claude-in-chrome` was left alone, as planned — it is enabled outside these two
files, so `/mcp` is the only way to turn it off and that is the human's call.
