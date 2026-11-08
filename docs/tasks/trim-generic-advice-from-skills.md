---
id: trim-generic-advice-from-skills
status: done
priority: high
---

Finding C-2 of `docs/ContextAuditFindings.md`.
`dynamic-form-backend/SKILL.md:88-142` and
`dynamic-form-frontend/SKILL.md:67-132` recite Effective Java, Clean Code,
Effective TypeScript and the React docs — about half of each body, and nothing
project-specific. Keep only the rules where this project deviates; move or drop
the rest.

# Implementation Plan

Checked whether anything in those blocks is load-bearing before moving them, and
nothing is: every project-specific rule inside them is already stated in the
sections above.

| Rule buried in the generic block | Already stated in |
| :- | :- |
| constructor injection, Lombok `@RequiredArgsConstructor` | backend **Coding Conventions** |
| favor standard exceptions | backend **Coding Conventions**, with the HTTP table |
| Lombok `@Value` for immutable DTOs, `@Component`/`@Service` singletons | backend **Coding Conventions** |
| use react-query, not `useEffect`, for fetching | frontend **Data access and caching** |
| use react-hook-form for forms | frontend **Forms and validation** |
| shared types live in `src/types` | frontend **Scope and defaults** / **Project map** |

So the blocks move whole, with nothing lifted out of them first.

Moved rather than deleted: the text goes to `references/` beside each skill, which
is not loaded when the skill is invoked but is there to read when a style question
actually comes up. Each skill gets a one-line pointer.

Steps:

1. `dynamic-form-backend/` — move the `## Clean Code Principles` and
   `## Effective Java` sections to `references/java-style.md`; truncate the skill
   after the Coding Conventions table; add the pointer line.
2. `dynamic-form-frontend/` — move the `## Effective TypeScript` and
   `## React Official Rules` sections to `references/ts-react-style.md`; truncate
   after **When adding a new page**; add the pointer line.
3. Check the line counts and that nothing was lost in the move.

Scope limit: the duplicated error table and project maps (C-8) are a separate
task and are left alone here.

No unit tests: the change moves instruction prose. Verification is the line
counts — moved-out lines plus retained lines must equal the originals.

# Outcome

Done. Two new reference files, two skills shortened:

| File | Before | After |
| :- | -: | -: |
| `dynamic-form-backend/SKILL.md` | 142 | 89 |
| `dynamic-form-backend/references/java-style.md` | — | 58 |
| `dynamic-form-frontend/SKILL.md` | 131 | 67 |
| `dynamic-form-frontend/references/ts-react-style.md` | — | 69 |

(Raw line counts including blanks. The audit's figures — 111 and 99 — came from
`Measure-Object -Line`, which does not count blank lines; both metrics are
consistent within themselves.)

Nothing was lost in the move: backend 88 retained + 54 moved = 142, the original
count. Frontend 64 retained + 65 moved = 129, two short of 131 because the `---`
separator and its blank line were deliberately dropped with the block they
separated.

Verified afterwards: grepping the two `SKILL.md` bodies for `Effective Java`,
`Clean Code`, `Effective TypeScript` and `React Official Rules` returns nothing;
both reference files open with their heading and the moved sections; both skills
carry a one-line pointer to their reference file.
