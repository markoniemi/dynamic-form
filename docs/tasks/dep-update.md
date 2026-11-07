---
id: dep-update
status: planned
priority: medium
kind: task
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

Each step is its own commit, and `/test` runs between steps so a failure points
at one upgrade rather than twenty.

1. **npm, in-range** — `npm update` in `frontend/`, which takes the 14 packages
   above and touches only `package-lock.json` plus the caret ranges. Run `/test`.
2. **Maven, safe** — `testcontainers.version` to 2.0.5. Run
   `mvn -pl backend -am verify` (this one needs the integration tests, since
   Testcontainers is what they use).
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
5. **npm majors, one commit each**, in rising order of risk:
   a. `prettier` to 3.9.10, then `npm run lint` — formatting-only, and it may
      reformat files, which should land as its own commit.
   b. `@testing-library/jest-dom` to 7.
   c. `vitest` and `@vitest/coverage-v8` to 5 together. Expect `clearMocks`
      fallout; fix the affected tests rather than setting `clearMocks: false`
      unless there are many, and say which was chosen.
   d. `i18next` 27 with `react-i18next` 18 together.
   e. `typescript` to 7, last, with `npm run compile` and `npm run lint`.
      `typescript-eslint` must support TS 7 or step 5e has to wait — see the open
      questions.
6. **Run the full suite** — `/test --with-integration`, which needs Docker. Also
   build the image once (`mvn -pl backend jib:dockerBuild`), because the
   `testcontainers` module's integration tests run against it and nothing in the
   unit suite would catch a break there.
7. **Update the docs the change makes wrong** — `README.md:3` says "Spring Boot
   3" and `:41-45` lists prerequisites; `backend/CLAUDE.md` names the Spring Boot
   and Java versions; `frontend/CLAUDE.md:3` names Vite and React versions and
   `:6` the Node version. Whichever of these a step above falsifies gets fixed in
   that step's commit.

## Open questions for the human

1. **Spring Boot.** The reactor report offered only `4.2.0-RC2` as newer than the
   current `4.0.3`, which suggests 4.0.3 may already be the newest stable — but I
   did not confirm that, and a Spring Boot minor bump moves the whole managed BOM
   (Hibernate, Jackson, Spring Security) in one go. Should this task bump Spring
   Boot at all, or stay on the 4.0.x line and leave the framework for its own
   task? **My recommendation: leave it out of this task.** It is the one change
   here that can alter runtime behaviour across the whole backend, and it deserves
   its own commit, its own test run and its own rollback.
2. **TypeScript 7.** Worth doing now, or worth waiting? It is a brand-new major
   and a whole new compiler implementation, and `typescript-eslint` 8.72 may not
   support it yet. If step 5e fails on the lint side, should it be dropped from
   this task or should `typescript-eslint` be moved to a pre-release that supports
   TS 7?
3. **Pre-releases.** I have assumed stable-only throughout, so `mapstruct`
   1.7.0.Beta2 and `maven-compiler-plugin` 4.0.0-beta-5 are excluded. Confirm.
4. **The two SNAPSHOT dependencies.** `interface-log:0.0.1-SNAPSHOT` and
   `auth-server-testcontainers:0.1-SNAPSHOT` cannot be "updated" — a snapshot is
   already whatever was published last, which is finding 11 of
   `docs/PonytailAuditFindings.md`. Out of scope here unless you want fixed
   versions published as part of this task.
