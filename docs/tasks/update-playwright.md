---
id: update-playwright
status: in_review
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

## Result

**Playwright is on 1.64.0, and the bug was ours.** `SubmissionDetailPage` had a
race that 1.64.0's timing exposed; nothing in Playwright regressed, so the
planned bisect (steps 3-4) was never needed and was not run.

The evidence, in the order it arrived:

1. `-Dit.test=FrontendIT` alone on 1.64.0: **3/3 pass**. So the failure needs
   `AdminRoleIT` to have run first — it is not a property of `FrontendIT`.
   (`-Dit.test` does keep the jib image build in `pre-integration-test`, so the
   fast loop in step 1 of the plan works as written.)
2. A temporary diagnostic inside `containsText` printed, immediately after
   `waitForLoad()` returned:

   ```
   DIAG url: http://localhost:8080/forms/submissions/2
   DIAG .card-body count: 0
   DIAG allTextContents: []
   DIAG textContent: Contact UsGet in touch…Your NameEmail Address…
   DIAG input/textarea count: 6
   ```

   `count()` says zero while `textContent()` returns the card's text, because
   **`textContent()` auto-waits and `count()` does not**. The card was still
   rendering when the method started.
3. With that diagnostic in place, all 4 ITs passed — the diagnostic's own round
   trips delay the next call enough to hide the bug. A Heisenbug, which is itself
   the proof that this is a race.
4. Diagnostic removed, same 1.64.0: failures returned.

So the mechanism: `waitForLoad()` waits for `//h2`, which appears before the
submission data resolves. `textContent()` on the card then auto-waits and returns
the labels — but the *values* live in inputs, because `ReadOnlyDynamicForm`
renders them as `Form.Control`. The method reached those inputs through
`count()`, which does not wait, so when the fields had not rendered yet the loop
ran zero times and the method returned `false`. 1.48.0 happened to lose that race
less often.

The fix retries the whole check until the timeout and splits the two lookups into
`cardTextContains` and `fieldValueContains`. `.first()` was added to the card
lookup as well, since `.card-body` is a class that may match more than one
element.

The retry is Playwright's own `Page.waitForCondition` (Java binding, since
v1.32), called directly. It dispatches Playwright events while it polls, which a
`Thread.sleep` loop — what `BasePage.sleep` does — cannot; Playwright's own
javadoc warns against the latter for exactly this. Its timeout comes from
`page.setDefaultTimeout(TIMEOUT_MS)`, set once in the `BasePage` constructor, so
no call site repeats it.

`containsText` became `waitForText` and returns nothing. The boolean it used to
return was wrapped in `assertTrue` at all nine call sites and nowhere else, so it
only converted Playwright's `TimeoutError` — which names the timeout — into
`expected: <true> but was: <false>`, which names nothing. Letting it throw
deleted the `BasePage.waitUntil` wrapper, the boolean and the nine `assertTrue`
calls. The unused `isDetailPresent` went with it, as it existed only to call
`containsText`.

Counts: 3 runs failed before the fix (2, 2 and 1 failures — the varying count is
the race), and **3 consecutive runs passed after it**, all on 1.64.0.

Also checked, nothing to do:

* `AdminRoleIT` (scope question 2) never touches a page object — it reads the
  access token out of `sessionStorage` and calls `/api/forms` over HTTP. It
  cannot have this bug.
* `FormSubmissionsPage.isSubmissionPresent` already guards its `count()` with a
  `waitForSelector`, which is why it was never flaky.
* `FormSubmissionsPage.getSubmissionCount` (`:51-54`) counts without waiting and
  has the same latent bug, but it has no caller anywhere in the test sources. Left
  alone rather than fixed or deleted, as neither is this task's job.

## How to review

Nothing is pushed — `create_pull_request` is off — so this is the commits on
`master`.

1. `git log -p 39451bc..HEAD -- backend/src/test/java/com/example/backend/e2e/`
   is the fix and the two rounds of trimming that followed it. The claim to check
   is the comment's: `Locator.count()` does not auto-wait, so the old single pass
   could run before the fields rendered.
2. `git show 39451bc -- backend/pom.xml` is the one-line bump to 1.64.0.
3. `/test --with-integration` from the repo root. Expect `BUILD SUCCESS` with 4
   ITs. **Run it more than once** — this was an intermittent failure, and a single
   green run would not have been evidence either way.
4. To see the old failure: `git stash` is not enough, since the pin and the page
   object must move together. Set `playwright` back to 1.64.0 with the old
   `containsText` body (it is in `git show HEAD~1:backend/src/test/java/com/example/backend/e2e/pages/SubmissionDetailPage.java`)
   and run the full `verify` — not `-Dit.test=FrontendIT`, which passes either
   way because `AdminRoleIT` must run first.

## Open questions for a human

Answered yes to all three on 2026-11-08:

1. **Rewriting `SubmissionDetailPage` is in scope.** The fix may change e2e test
   code, not only the version pin.
2. **`AdminRoleIT` is in scope** if the same racy page object turns out to affect
   it.
3. **A newer Playwright is worth the runs.** Steps 3-6 are on; step 7 is the
   fallback, not the expected outcome.
