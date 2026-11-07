---
id: dependency-update
status: in_review
priority: medium
kind: feature
---

- update maven and npm dependencies, use context7.

# Implementation Plan

## What is actually out of date

**npm** — `npm outdated` in `frontend/` reports 21 packages, which split cleanly:

*14 are within the existing version range* (`Wanted` equals `Latest`), so a plain
`npm update` takes them: `@tanstack/react-query`, `@testing-library/user-event`,
`@vitejs/plugin-react`, `eslint`, `globals`, `i18next-browser-languagedetector`,
`i18next-http-backend`, `lucide-react`, `react`, `react-dom`, `react-hook-form`,
`react-router-dom`, `typescript-eslint`, `vite`.

*7 need a deliberate decision:*

| Package | From | To | Note |
| :- | :- | :- | :- |
| `typescript` | 6.0.3 | 7.0.2 | major; the native Go compiler |
| `vitest` | 4.1.11 | 5.1.0 | major; must move with its coverage package |
| `@vitest/coverage-v8` | 4.1.5 | 5.1.0 | major; pinned to vitest's major |
| `react-i18next` | 17.0.15 | 18.0.0 | major; dictates the minimum `i18next` |
| `i18next` | 26.4.2 | 27.0.1 | major; pairs with the above |
| `@testing-library/jest-dom` | 6.9.1 | 7.0.1 | major |
| `prettier` | 3.8.3 | 3.9.10 | minor, but pinned exactly, so it needs an edit |

**Maven** — `versions:display-dependency-updates` on the whole reactor mostly
reports Spring Boot's managed BOM, which this repo does not control. Only these
versions are pinned here:

| Pin | Current | Where |
| :- | :- | :- |
| `spring-boot-starter-parent` | 4.0.3 | `pom.xml:18` |
| `mapstruct.version` | 1.5.5.Final | `pom.xml:34` |
| `maven-compiler-plugin.version` | 3.11.0 | `pom.xml:35` |
| `testcontainers.version` | 2.0.3 | `pom.xml:36` |
| `lombok-mapstruct-binding` | 0.2.0 | `pom.xml:87` |
| `jib-maven-plugin` | 3.3.2 | `pom.xml:100` |
| `frontend-maven-plugin` | 1.15.4 | `frontend/pom.xml:29` |
| `maven-resources-plugin` | 3.3.1 | `frontend/pom.xml:53` |
| `postgresql` | 42.7.2 | `backend/pom.xml:68` |
| `jackson-dataformat-yaml` | 2.18.1 | `backend/pom.xml:132` |
| `playwright` (Java) | 1.48.0 | `backend/pom.xml:143` |
| `jacoco-maven-plugin` | 0.8.14 | `backend/pom.xml:194` |
| `interface-log` | 0.0.1-SNAPSHOT | `backend/pom.xml:59` |
| `auth-server-testcontainers` | 0.1-SNAPSHOT | `backend/pom.xml:126`, `testcontainers/pom.xml:32` |

Of the newest versions the plugin offered, only `testcontainers` 2.0.3 → 2.0.5 is
a stable release. `mapstruct` offered 1.7.0.Beta2 and `maven-compiler-plugin`
offered 4.0.0-beta-5 — both pre-release, so each needs its latest *stable*
looked up instead.

## What context7 says about the risky ones

Three majors were checked against the libraries' own docs.

**TypeScript 7** removes options that only warned in 6: `module=AMD`,
`moduleResolution=classic` and `node10`, `outFile`, and `target=ES5`. I checked
this project against that list — `tsconfig.app.json` and `tsconfig.node.json` use
`target` ES2020/ES2022, `module: ESNext` and `moduleResolution: bundler`, so
**none of the removed options are in use** and the config needs no changes. The
remaining risk is the toolchain around it, not the config.

**Vitest 5** changes a default that this project relies on implicitly:
`clearMocks` is now `true`, so `vi.clearAllMocks()` runs before every test and
mock call history no longer accumulates across tests. `vitest.config.ts` does not
set `clearMocks`, so the new default will apply to all 52 frontend tests. Any test
asserting a call count that built up over more than one test will start failing.
Setting `clearMocks: false` restores the old behaviour in one line, if the
failures turn out to be widespread. Also renamed: `test.workspace` →
`test.projects`, and `browser.api` → top-level `api`; this project uses neither.

**react-i18next 18 / i18next 27** must move together. The docs confirm the
pairing rule rather than the v18 change list: each react-i18next line sets a
minimum `i18next` peer version (v17 required i18next >= 26.0.1). I could not
retrieve v18's own breaking-change list from context7, so that one stays a
genuine unknown — see the open questions.

## Steps

One upgrade per step, one commit per step, so a failure points at one upgrade
rather than twenty.

**After every step: `/test --with-integration`** (`mvn -pl backend -am verify`,
Docker required). Decided by the human on 2026-11-07, over the faster unit-only
`/test`. It means the backend integration tests also run after the npm-only
steps, so the whole task is roughly twelve Testcontainers runs — slow on purpose.
A step does not get committed until that run is green; if it is red, fix it
inside that step or revert that step alone.

1. **npm, in-range** — `npm update` in `frontend/`, which takes the 14 packages
   above and touches only `package-lock.json` plus the caret ranges.
2. **Testcontainers** — `testcontainers.version` 2.0.3 → 2.0.5.
3. **Maven, delete rather than bump** — `postgresql` (`backend/pom.xml:67-70`) and
   `jackson-dataformat-yaml` (`:130-134`) both pin a version that Spring Boot's
   BOM already manages, which is why they have drifted. Remove the `<version>`
   from both and let the BOM decide. For `jackson-dataformat-yaml` this matters
   more than tidiness: it is a Jackson **2** artifact, while the application code
   is on Jackson 3 (`DatabaseInitializer.java:5` imports
   `tools.jackson.databind.ObjectMapper`), so the test code is pulling in the
   previous major of a library the main code has already moved past. If removing
   the version fails to resolve, pin the Jackson 3 equivalent instead.
4. **Maven plugins and MapStruct** — look up the latest *stable* of each and bump:
   `mapstruct.version`, `maven-compiler-plugin.version`, `jib-maven-plugin`,
   `frontend-maven-plugin`, `maven-resources-plugin`, `jacoco-maven-plugin`,
   `lombok-mapstruct-binding`, and the Java `playwright` used by the e2e tests.
   No beta or RC. MapStruct and `lombok-mapstruct-binding` work together with
   Lombok in `pom.xml:78-94`, so bump them in one commit and check that the
   generated mappers still compile — `FormMapperTest` and `FormDataMapperTest`
   are the tests that prove it.
5. **Spring Boot parent** — `pom.xml:18`, currently 4.0.3. Resolve the latest
   *stable* 4.x from Maven Central first (`4.2.0-RC2` was the only newer version
   the reactor report offered, and an RC is excluded), and bump to that; if 4.0.3
   is already the newest stable, record that and skip the step. This moves the
   whole managed BOM — Hibernate, Jackson, Spring Security — so it is the one step
   that can change backend runtime behaviour, and it comes after step 3 so the
   two pins removed there take their versions from the new BOM. Done early rather
   than last on purpose: everything after it is frontend-only, so a regression
   here is not hidden behind npm churn. Watch for Hibernate schema differences
   against H2 and for resource-server / JWT changes — those must be verified
   against `../../oauth2-server` (repo rule).
6. **prettier** 3.8.3 → 3.9.10, then `npm run lint`. Formatting-only, but it may
   reformat files; that reformatting lands in this step's commit.
7. **@testing-library/jest-dom** 6.9.1 → 7.
8. **vitest + @vitest/coverage-v8** 4.1.x → 5.1.0, together — the coverage package
   is pinned to vitest's major. Expect `clearMocks` fallout (now `true` by
   default). Fix the affected tests rather than setting `clearMocks: false`,
   unless there are many; say which was chosen.
9. **i18next 27 + react-i18next 18**, together — the peer range forces it. The v18
   breaking-change list could not be retrieved from context7, so this is the step
   most likely to need unplanned work; read the upstream changelog at the start of
   the step.
10. **typescript** 6.0.3 → 7, last, with `npm run compile` and `npm run lint`.
    None of the options TS 7 removes are in use (see above), so the risk is the
    toolchain: if `typescript-eslint` 8.72 does not support TS 7, **revert this
    step and leave TypeScript on 6** — do not move `typescript-eslint` to a
    pre-release. Decided by the human on 2026-11-07.
11. **App image and the `testcontainers` module** — `mvn -pl backend
    jib:dockerBuild`, then the `testcontainers` module's own tests. Nothing in
    steps 1-10 covers these: `/test --with-integration` builds only `backend -am`,
    so a break in `DynamicFormContainer` / `DynamicFormStack` would go unseen.
12. **Update the docs the change makes wrong** — `README.md:3` says "Spring Boot
    3" and `:41-45` lists prerequisites; `backend/CLAUDE.md` names the Spring Boot
    and Java versions; `frontend/CLAUDE.md:3` names Vite and React versions and
    `:6` the Node version. Whichever of these a step above falsifies gets fixed in
    that step's commit; this step is the sweep for whatever was missed.

## Findings while implementing

**Step 4 — Playwright (Java) 1.48.0 stays.** 1.64.0 was tried and reverted:
`FrontendIT.loginAndFormSubmission` (`:66`) and `FrontendIT.editSubmission`
(`:102`) both fail on the final `submissionDetailPage.containsText(...)`
assertion, reproducibly over two runs. The same verify is green with 1.48.0 and
every other step-4 bump in place, so Playwright is the cause, not the plugin
bumps. Not investigated further — it is a test-only dependency and nothing in
this task needs a newer one. A human who wants 1.64 should look at
`SubmissionDetailPage.containsText` (it calls `textContent()` on the details
card) and at the one-off browser download 1.64 triggers, which made
`AdminRoleIT` take 138 s on the first run.

**Step 10 — TypeScript 6.0.3 stays.** 7.0.2 installs and `tsc --noEmit` passes
on this project's sources, so the config really was clean as planned. `npm run
lint` is what blocks it:

```
Error: typescript-eslint does not support TS 7.0.
See also https://github.com/typescript-eslint/typescript-eslint/issues/10940 for tracking typescript-eslint's support for TS >=7.1
```

Reverted per the human's decision of 2026-11-07, which excluded moving
`typescript-eslint` to a pre-release. Retry once typescript-eslint ships TS 7
support; the only other option upstream names is running typescript-eslint
against the TS 6 API side by side, which is more machinery than this is worth.

**Step 5 — Spring Boot 4.0.3 → 4.1.2**, the newest stable 4.x (4.2.0-RC2
excluded). The first build failed on `error reading
target/generated-sources/annotations/.../FormListItemMapperImpl.java`; a `clean`
fixed it, so it was a stale incremental artifact from the MapStruct bump, not a
Spring Boot break. No Hibernate/H2 or JWT fallout — the resource-server ITs
against `../../oauth2-server` pass unchanged.

Versions resolved from `maven-metadata.xml` on Central, latest stable only:
mapstruct 1.6.3 (1.7.0.Beta2 skipped), maven-compiler-plugin 3.16.0
(4.0.0-beta-5 skipped), jib 3.5.2, frontend-maven-plugin 2.0.2,
maven-resources-plugin 3.5.0 (4.0.0-beta-2 skipped), jacoco 0.8.16.
`lombok-mapstruct-binding` 0.2.0 is already the latest release.

## Result

One commit per step on `master`, each one green on `/test --with-integration`
before it was committed. Twelve steps planned, ten changed something:

| Step | Outcome |
| :- | :- |
| 1 | 14 in-range npm packages updated; `package-lock.json` only |
| 2 | `testcontainers` 2.0.3 → 2.0.5 |
| 3 | `postgresql` and `jackson-dataformat-yaml` pins deleted; the BOM gives 42.7.10 and 2.20.2 |
| 4 | mapstruct 1.6.3, maven-compiler-plugin 3.16.0, jib 3.5.2, frontend-maven-plugin 2.0.2, maven-resources-plugin 3.5.0, jacoco 0.8.16. **Playwright stayed on 1.48.0** |
| 5 | Spring Boot parent 4.0.3 → 4.1.2, plus the two docs it falsified |
| 6 | prettier 3.8.3 → 3.9.10; no reformatting (see below) |
| 7 | `@testing-library/jest-dom` 6.9.1 → 7.0.1 |
| 8 | vitest + `@vitest/coverage-v8` → 5.1.0; one-line fix in `test/setup.ts` |
| 9 | i18next 27.0.1 + react-i18next 18.0.0; no config change |
| 10 | **No change** — TypeScript stayed on 6.0.3 |
| 11 | App image and the `testcontainers` module checked: `DynamicFormStackIT` 4/4 |
| 12 | Nothing further to fix; `README.md:3` and `backend/CLAUDE.md:3` were corrected in step 5 |

Final suite: 3 api, 52 frontend, 73 backend unit, 4 backend IT, 4 testcontainers
IT — all passing.

Two things the plan expected that did not happen. The Vitest 5 `clearMocks: true`
default broke nothing, so `vitest.config.ts` is untouched; what broke instead was
`tsc`, because vitest 5 types custom matchers differently and the plain
`@testing-library/jest-dom` import no longer augments `Assertion`. And prettier
3.9.10 reformats nothing new: it reports the same 101 files as 3.8.3, so that
drift predates this task, and `npm run ci` does not check formatting anyway.

Left undone, deliberately: Playwright (Java) 1.64.0 and TypeScript 7 (both above),
and the two SNAPSHOT dependencies (note 5 below). `npm audit` reports 9 moderate
advisories, all one `minimatch` ReDoS reached through `eslint` and
`typescript-eslint`; `npm audit fix` cannot resolve it without `--force`, it is
lint-only tooling that never ships, and it was not in this task's scope.

## How to review

Nothing is pushed and no request is open — `create_pull_request` is off — so this
is the commits on `master`, `4b4740f..HEAD`.

1. `git log --oneline 4b4740f..HEAD` — ten commits, one per step, in plan order.
   Each message says what moved and why.
2. `git diff 4b4740f..HEAD -- pom.xml backend/pom.xml frontend/pom.xml` is the
   whole Maven change: 10 version lines and 2 deletions. The deletions in
   `backend/pom.xml` are step 3 — confirm with
   `mvn -pl backend dependency:tree "-Dincludes=org.postgresql:postgresql"`, which
   prints 42.7.10 from the BOM.
3. `git diff 4b4740f..HEAD -- frontend/test/setup.ts` is the only source file this
   task changed: one import, `@testing-library/jest-dom` →
   `@testing-library/jest-dom/vitest`.
4. `/test --with-integration` from the repo root. Expect `BUILD SUCCESS` and the
   counts above. It takes about four minutes and needs Docker.
5. `mvn -pl testcontainers -am verify` — the step 11 check, not covered by the
   above. Expect `DynamicFormStackIT` 4/4.
6. To see the two reverts for yourself: `cd frontend; npm install typescript@7;
   npm run lint` prints `typescript-eslint does not support TS 7.0`. Undo with
   `git checkout -- frontend/package.json frontend/package-lock.json; npm install`.
   The Playwright one costs a full `verify` run, so the failing assertions are
   quoted in **Findings while implementing** instead.

## Decisions and remaining notes

Answered by the human on 2026-11-07:

1. **Spring Boot is in** — step 5, latest stable 4.x, no RC.
2. **TypeScript 7 is in** — step 10, dropped if `typescript-eslint` blocks it.
3. **Full suite after each step** — `/test --with-integration`, not unit-only.

Still assumed, not asked:

4. **Pre-releases are excluded** throughout, so `mapstruct` 1.7.0.Beta2 and
   `maven-compiler-plugin` 4.0.0-beta-5 are out; each gets its latest stable.
5. **The two SNAPSHOT dependencies stay.** `interface-log:0.0.1-SNAPSHOT` and
   `auth-server-testcontainers:0.1-SNAPSHOT` cannot be "updated" — a snapshot is
   already whatever was published last, which is finding 11 of
   `docs/PonytailAuditFindings.md`. Out of scope here.
