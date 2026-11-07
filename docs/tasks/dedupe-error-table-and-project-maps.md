---
id: dedupe-error-table-and-project-maps
status: in_review
priority: medium
---

Finding C-8 of `docs/ContextAuditFindings.md`. The exception → HTTP table appears
three times across the backend and fullstack skills, and the skills repeat
project maps that the module `CLAUDE.md` files already own. Single-source each;
C-1 is this duplication already failing.

# Implementation Plan

The three copies are not equivalent, which the finding missed:

| Copy | Entries |
| :- | :- |
| `dynamic-form-fullstack` RFC 7807 section | 6 — includes `MethodArgumentNotValidException` → 400 and the 500 fallback |
| `dynamic-form-fullstack` API contract section | 4 — a subset of the above, same file |
| `dynamic-form-backend` Coding Conventions | 4 — same subset, as a table |

**Within `dynamic-form-fullstack` the duplication is pure waste** — the same file
states the mapping twice, the second time less completely. That one gets replaced
by a pointer to the section above it.

**Across the two skills it is a real trade-off, and I am not following the
finding here.** The finding says fullstack should own the table and the backend
copy should go. But a backend-only task is the common case and it would then have
to load the fullstack skill to find the project's central error contract. Deleting
either copy leaves a gap at one of the two entry points.

So instead of removing a copy, the drift mechanism is removed: both copies get the
same six entries, and each says that `controller/GlobalExceptionHandler` is the
source of truth. Drift then shows up against the code rather than silently
between two skills — which is what actually went wrong in C-1.

**The project maps are straightforward.** `dynamic-form-backend`'s "Project
Structure" and `dynamic-form-frontend`'s "Project map" restate what
`backend/CLAUDE.md:10-14` and `frontend/CLAUDE.md:11-16` already own, and those
load when the module's files are touched. Both get replaced by a pointer, keeping
only the facts the module file does not carry.

Steps:

1. `dynamic-form-fullstack/SKILL.md` — API contract section's error list becomes a
   pointer to the RFC 7807 section; that section gains the source-of-truth line.
2. `dynamic-form-backend/SKILL.md` — error table extended to six entries to match,
   plus the same source-of-truth line; "Project Structure" replaced by a pointer
   to `backend/CLAUDE.md`, keeping the "DTOs are in `api`" line and the profile
   list, which that file states only in passing.
3. `dynamic-form-frontend/SKILL.md` — "Project map" replaced by a pointer to
   `frontend/CLAUDE.md`.
4. Check the line counts and that no mapping entry was lost.

The RFC 7807 one-liners in the four `CLAUDE.md` files are left alone, as the
finding says: cheap, and load-bearing where they are.

No unit tests: instruction prose.

# Outcome

Done, three files changed:

* `dynamic-form-fullstack/SKILL.md` — the API contract section's four-entry list
  is now a pointer to the RFC 7807 section in the same file; that section names
  `controller/GlobalExceptionHandler` as the source of truth and says the backend
  skill carries a twin.
* `dynamic-form-backend/SKILL.md` — the error table is now the full six entries,
  matching fullstack, with the same source-of-truth line; "Project Structure" is
  a pointer to `backend/CLAUDE.md` keeping the three facts that file does not
  state (DTOs in `api`, the profile file list, `IntegrationTestBase`).
* `dynamic-form-frontend/SKILL.md` — "Project map" is a pointer to
  `frontend/CLAUDE.md`, keeping `App.tsx` and `main.tsx`, which it does not name.

Net effect: one duplicate removed outright, one pair made identical and tied to
the code, two project maps reduced to what the module files do not cover.
Backend skill 3943 → 3916 bytes, frontend 3036 → 2833.

## Encoding defect found and fixed

While editing I hit a mismatch that turned out to be corruption **I introduced in
the `trim-generic-advice-from-skills` task**: that task split the files with
PowerShell, and `Get-Content -Raw` in PowerShell 5.1 read the UTF-8 em dashes as
three ANSI characters, which `Set-Content -Encoding utf8` then wrote back
faithfully. 18 em dashes across four files became mojibake, and the files gained a
BOM the other skill files do not have. The corruption was committed in `d483c87`.

Worse, the first repair attempt made it briefly much worse: a PowerShell
`String.Replace` with a three-character `oldChar` threw, the variable it should
have assigned stayed `$null`, and `[System.IO.File]::WriteAllText` then wrote
**four empty files**. `git checkout --` restored all four from `d483c87`; nothing
was lost because the only uncommitted edits at that moment were in
`dynamic-form-fullstack/SKILL.md`, a file not in that set.

Both repairs were then done with the Edit tool, which handles the encoding
correctly. Verified afterwards: no `U+00E2` in any skill file, em dashes present,
and no file is zero bytes.

Lesson worth keeping: edit these files with the Edit tool, not with
`Get-Content`/`Set-Content` round-trips in PowerShell 5.1.
