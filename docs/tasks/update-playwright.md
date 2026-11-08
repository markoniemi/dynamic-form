---
id: update-playwright
status: planned
priority: medium
kind: task
---

Split out of `dependency update`, which reverted it. Playwright (Java) 1.48.0 →
1.64.0 makes `FrontendIT.loginAndFormSubmission` (`:66`) and
`FrontendIT.editSubmission` (`:102`) fail on their final
`submissionDetailPage.containsText(...)` assertion, reproducibly, while the same
verify is green on 1.48.0. Find out why and bump, or record that we stay on
1.48.0. Start at `SubmissionDetailPage.containsText`, which calls `textContent()`
on the details card. Note 1.64 downloads new browsers on its first run — that is
what made `AdminRoleIT` take 138 s, not a test failure.
- perhaps test 1.61.0 first.

# Implementation Plan

## What is known

From the `dependency update` task, two runs, same result both times:

* `backend/pom.xml:141` is the only pin — `com.microsoft.playwright:playwright`,
  test scope. Latest stable is 1.64.0.
* With 1.64.0: `FrontendIT` 3 tests, 2 failures. `AdminRoleIT` passes.
* Both failures are `AssertionFailedError: expected: <true> but was: <false>` on
  the last `containsText` of each test — `FrontendIT:66` ("Test User") and
  `FrontendIT:102` ("Test User Updated").
* **Failures, not errors.** So `containsText` returned `false`; it did not throw.
  That rules out a strict-mode violation on the `.card-body` locator, which would
  have surfaced as an error.
* `loginAndFormSubmission` reported 0.794 s elapsed, which is far too short for
  the login-and-submit flow it performs. Unexplained, and worth treating as a
  clue rather than noise.
* Reverting to 1.48.0, with every other step-4 bump still in place, is green. So
  this is Playwright, not MapStruct/jib/jacoco/the plugins.

The suspect method, `SubmissionDetailPage.containsText` (`:23-41`):

```java
if (page.locator(DETAILS_CARD).textContent().contains(text)) { ... }
Locator inputs = page.locator(DETAILS_CARD + " input, " + DETAILS_CARD + " textarea");
for (int i = 0; i < inputs.count(); i++) { ... inputs.nth(i).inputValue() ... }
```

Two things about it matter. `DETAILS_CARD` is `.card-body`, a class that may match
more than one element on the page, and `textContent()` is a one-shot read with no
auto-waiting — unlike `assertThat(locator).containsText(...)`, which retries.
Either could change behaviour across 16 minor versions without being a Playwright
bug.

## Steps

The point of step 1 is to stop guessing: the failure message tells us the method
returned `false`, not what the page actually held.

1. **Capture what the page really contained.** Set `playwright` to 1.64.0 and run
   only the failing class, then read the evidence rather than the assertion:

   ```powershell
   mvn -pl backend -am verify "-Dit.test=FrontendIT" "-Dsurefire.skip=true"
   ```

   `maven-failsafe-plugin` is declared at `backend/pom.xml:177-188` with no
   configuration, so `-Dit.test` is the stock filter. Confirm the jib image still
   builds in `pre-integration-test` under that filter; if the filter skips the
   image, drop `-Dit.test` and pay the full run.

   For the evidence itself, prefer Playwright's own tracing over print
   statements: `BrowserType.newContext` + `context.tracing().start(...)` in
   `PlaywrightConfig`, written to `target/trace.zip` on failure. A trace gives the
   DOM at the moment of the call. If wiring tracing turns out to be more than a
   few lines, fall back to logging `page.locator(DETAILS_CARD).count()`,
   `allTextContents()` and each `inputValue()` inside `containsText`, and delete
   the logging before the task closes.

   Answer three questions: how many `.card-body` elements match, what their text
   is, and whether the submission detail had rendered at all when the call ran.

2. **Explain the 0.794 s.** If the trace shows the flow never reached the detail
   page, the real failure is earlier than line 66 and `containsText` is only where
   it surfaced. That changes the fix, so settle it before touching the page object.

3. **Bisect, but only if step 1 leaves the cause open.** Try 1.61.0 first, as the
   backlog entry suggests, then halve between 1.48.0 and the first failing
   version. Each attempt is one `-Dit.test=FrontendIT` run. Record each
   version and its result in this file as you go — the table is the deliverable
   even if the bump is abandoned.

4. **Read the release notes for the first failing version**, once bisect names it.
   Look specifically for changes to `textContent()`, `inputValue()`, locator
   strictness, and the bundled Chromium version — a Chromium behaviour change is
   as likely as an API change, and the fix differs.

5. **Fix the root cause, which is probably the page object.** If step 1 shows
   `.card-body` matching several elements, or the text arriving after the call,
   then the test was always racy and 1.64.0 merely exposed it. The fix is in
   `SubmissionDetailPage`, not in a version pin:
   * `assertThat(page.locator(DETAILS_CARD)).containsText(text)` for the
     auto-waiting assertion, or
   * `allTextContents()` instead of `textContent()` if several cards are legitimate.

   Whatever the shape, it keeps the existing input/textarea fallback — the
   read-only form renders values into inputs, so dropping that branch would break
   the passing assertions too.

6. **Bump and verify.** `playwright` to the highest version that passes, then
   `/test --with-integration` for the full suite. Download the new browsers once
   before timing anything, so the first-run download does not look like a hang
   (it is what made `AdminRoleIT` take 138 s).

7. **If no version above 1.48.0 passes with a page object that is correct**, stay
   on 1.48.0 and write down here which versions were tried and what the trace
   showed. That is a complete outcome for this task, not a failure — the entry
   says "or record that we stay on 1.48.0".

## Open questions for a human

1. **Is rewriting `SubmissionDetailPage` in scope?** Step 5 changes e2e test code,
   not a version pin. If the answer is no, the task can only bisect and report,
   and will likely end at step 7.
2. **Is `AdminRoleIT` in scope if it turns out to be affected too?** It passed on
   1.64.0, but it drives the same UI through the same page objects, so a racy
   page object may be failing silently there (it asserts less).
3. **How much is a newer Playwright worth here?** It is a test-only dependency.
   If the honest answer is "little", step 7 is the cheap outcome and steps 3-6 are
   not worth the runs. Say so and the task closes at a decision record.
