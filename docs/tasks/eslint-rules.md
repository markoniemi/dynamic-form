---
id: eslint-rules
status: done
priority: medium
kind: task
---

- Add no-misused-promises, no-floating-promises and no-base-to-string to eslint.

## Result

These three are type-aware rules, so ESLint needed type information it was not
getting: `frontend/eslint.config.js` used `tseslint.configs.strict` with no
`parserOptions.project`, which parses each file alone. Added
`parserOptions.projectService: true` plus the three rules.

`tseslint.configs.strictTypeChecked` was measured first and deliberately not
used: it reports 106 findings here, of which about 100 are test idioms
(43 `unbound-method`, from `expect(obj.method)`) or style
(`no-confusing-void-expression`, `restrict-template-expressions`).

`no-misused-promises` is configured `checksVoidReturn: { attributes: false }`.
All 13 of its findings were an async `onClick`/`onSubmit` or react-hook-form's
`handleSubmit()` passed to a JSX attribute, which is the documented usage for
both libraries. Without that option the rule is pure noise in a React codebase.

Ten call sites then had to be fixed. Two were real:

- `Navigation.tsx:15` — `signoutRedirect()` was unawaited, so a rejection left
  the user signed in with no sign of it. Now logs.
- `ReadOnlyDynamicForm.tsx` — submission values are `unknown`, and `String()` on
  an object renders `[object Object]` into a read-only field. Added `asText`,
  which JSON-stringifies anything that is not a string, number or boolean, and a
  test for it.

The other eight were conventional and are now marked as such: `void` on
`queryClient.invalidateQueries()` and on react-router's `navigate()` (it returns
a promise in v7), a `.catch` on `i18n.changeLanguage()` and on the `i18n.init()`
chain, and `url.url` instead of `url.toString()` on a `Request` in
`test/setup.ts`.

Two fixes outside the three rules, both needed to make lint pass at all:

- `tsconfig.node.json` now includes `vitest.config.ts`. The project service
  could not find it in any tsconfig, which is a parsing error once type-aware
  parsing is on.
- `eslint.config.js` ignores `target`, where the Maven build puts the bundled
  SPA. ESLint was linting the minified output.

Cost: `npx eslint .` goes from 7 s to 9 s. The Stop hook runs frontend lint, so
that lands on every turn touching the frontend.

Tests: 53 frontend (one new), 73 backend unit, 3 api, 4 backend IT — all pass.

## How to review

Nothing is pushed — `create_pull_request` is off — so this is the commits on
`master`.

1. `git show --stat HEAD` for the shape: config, four `src` files, two test
   files, two tsconfig/eslint lines.
2. `git diff HEAD~1 -- frontend/eslint.config.js` is the actual decision: three
   rules, one option, and why `strictTypeChecked` was not used.
3. The judgement call worth checking is `ReadOnlyDynamicForm.tsx`. Decide whether
   a JSON blob in a read-only text field is the right rendering for an object
   value, or whether it should be `common.empty` instead. `npx vitest run
   test/components/ReadOnlyDynamicForm.test.tsx` covers it — look at
   "renders an object value as JSON, not [object Object]".
4. `cd frontend; npm run lint; npm run compile` — both clean, lint takes about
   9 s.
5. `/test --with-integration` from the repo root for the e2e proof: the rules
   touched `Navigation`, `EditForm`, `FormSubmission` and
   `ReadOnlyDynamicForm`, which `FrontendIT` drives. Expect `BUILD SUCCESS`.
6. To see what was rejected, put `...tseslint.configs.strictTypeChecked` in
   `extends` and run lint: 106 findings, the breakdown above.
