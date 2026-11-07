---
id: review-ponytail-codebase-map
status: in_review
priority: low
---

Finding C-6 of `docs/ContextAuditFindings.md`. The ponytail SessionStart hook
injects a codebase map every session (~280 tok) that overlaps the Layout sections
in each module `CLAUDE.md` and goes stale silently. Decide whether it earns its
place. Outside the repo — user-level config.

# Implementation Plan

This one is a decision, not an edit, so the work was reading the hook and
comparing what it produces against what the module files already say.

What the hook is:
`~/.claude/plugins/cache/ponytail/ponytail/5.2.0/hooks/ponytail-map.js`. It runs
`git ls-files` plus one regex per language and prints top-level functions,
classes and exports, one line per folder, inside a character budget. It skips
tests, generated and vendored directories.

How it can be controlled: `PONYTAIL_MAP_CHARS`, default 2000, read as
`Number(process.env.PONYTAIL_MAP_CHARS || 2000)`. There is **no off switch** —
and note that setting it to `0` does not work, because `0` is falsy in JavaScript
and falls through to the 2000 default. Suppressing it would mean setting the
budget to `1`, which is a hack against an implementation detail, or disabling the
whole ponytail plugin, which the `prune unused plugins` task found is in use.

# Outcome

**Decision: keep it. No change made.**

The finding assumed it duplicates the module `CLAUDE.md` Layout sections. It does
not. Those sections say what each directory is *for* — "`src/pages/` routed
screens", "`controller/` → `service/` → `repository/`". The map lists the actual
symbols: `FormDataService`, `FormService`, `SecurityUtils`, `RoleClaimsConverter`,
the component names. Knowing a class is already called `SecurityUtils` is what
stops a search, and no instruction file in this repo carries that.

At roughly 280 tokens a session it is the cheapest item in the whole audit, and
the audit itself rated it `low` and said it was "acceptable if it genuinely
prevents exploration". It does.

The staleness concern in the finding is also backwards: the map is regenerated
from `git ls-files` at every session start, so it cannot go stale the way a
hand-written map in a `CLAUDE.md` can. That part of the finding was wrong.

Recorded for later, if the budget ever needs cutting: `PONYTAIL_MAP_CHARS` takes a
character count, and `1` is the practical minimum because `0` is ignored.
