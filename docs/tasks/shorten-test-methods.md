---
id: shorten-test-methods
status: in_review
priority: medium
kind: task
---

- FormSubmissionPage.fillTextArea could be written as
```java
  public void fillTextArea(String fieldName, String value) {
  page.fill("textarea[name='%s']".formatted(fieldName), value);
}
```
- is waitForSelector required? looks like overkill to wait for selector in every method.
  loginAndFormSubmission test calls formSubmissionPage.waitForLoad(); before setting input values.

Merged in the `FindBy annotation` entry: *Selenium has FindBy annotation, using it makes easy to
act on page elements. Does playwright have something similar?* Both rewrite the same five page
objects, so they are one change.

# Implementation Plan

Two questions, both answered before any code moves.

**Is `waitForSelector` required before an action?** No. Playwright's actions (`fill`, `click`,
`selectOption`, `textContent`) auto-wait for actionability up to the context default timeout, which
this project sets once on the browser context (commit e5bdf02). A `waitForSelector` on the same
selector waits twice. The exceptions are the query methods that read the page as it is right now
and never wait — `Locator.count()`, `isVisible()` — and those keep their waits.

**Does Playwright have `@FindBy`?** No annotation, and nothing to configure: no `@FindBy`, no
`PageFactory.initElements`, no annotation processor. `Locator` is what replaces it.
`page.locator(...)` does not touch the DOM — it stores the query and re-resolves it on every call,
so one instance never goes stale across re-renders or navigations. That is why Selenium needs an
injected proxy and Playwright needs nothing: a plain `final Locator` field already *is* the lazy,
reusable element handle, and because every call auto-waits there is no `WebDriverWait` to pair with
it. So the page objects stop keeping selector `String`s and keep `Locator`s.

Scope: `backend/src/test/java/com/example/backend/e2e/pages/` only. The IT bodies change only
where a deleted method or a changed signature forces it, and no selector text changes.

## Do, in this order

Delete first, so nothing dead gets converted.

1. **Delete the dead members.** A grep over `backend/src` finds a declaration and no caller for
   `FormSubmissionsPage.getSubmissionCount`, `FormSubmissionPage.isSubmitDisabled`,
   `FormSubmissionPage.cancel` and `FormsPage.openFirstForm`, and no use at all of the constants
   `FormSubmissionPage.CANCEL_BUTTON` (only `cancel` used it) and `FormSubmissionPage.ERROR_ALERT`
   (never used). All six go.
2. **Drop the redundant waits.**
   * `FormSubmissionPage` — the `waitForSelector` in `fillTextField`, `fillTextArea`,
     `selectOption`, `selectRadio`, `checkCheckbox`, and the `waitForLoad()` in `getFormTitle`
     (`textContent` waits for the title itself). `fillTextArea` also fills a second, freshly built
     copy of its own selector; that copy goes with it.
   * `LoginPage.login` — the `waitForSelector(USERNAME_INPUT)`; the `fill` after it waits for that
     same input.
   * `FormSubmissionsPage` — the `waitForLoad()` in `getFirstSubmissionId`, `viewFirstSubmission`
     and `editFirstSubmission`. Each then waits on the table row it actually touches, a stronger
     guarantee than the heading it waits on today.
3. **Selector constants → `Locator` fields.** In `LoginPage`, `FormsPage`, `FormSubmissionPage`,
   `FormSubmissionsPage` and `SubmissionDetailPage`, each surviving
   `private static final String X = "<selector>"` becomes
   `private final Locator x = page.locator("<selector>");` with the same selector text, and the
   bodies use the field: `page.click(LOGIN_BUTTON)` → `loginButton.click()`,
   `page.textContent(FORM_TITLE)` → `formTitle.textContent()`. A subclass field initializer runs
   after `BasePage`'s constructor has assigned `page`, so this needs no constructor body.
4. **Parameterized selectors stay methods**, returning a `Locator` instead of a `String`:
   `private Locator textField(String name) { return page.locator("input[name='%s']".formatted(name)); }`
   and the same for textarea, select, radio and checkbox. With steps 2 and 3 done, each public
   `fill*`/`select*` body is the one line the entry asks for —
   `textField(fieldName).fill(value)`.
5. **`SubmissionDetailPage`** — mechanical swap only. `page.locator(DETAILS_CARD + " input, " +
   DETAILS_CARD + " textarea")` becomes `detailsCard.locator("input, textarea")`, which is the same
   query without the string surgery. The `waitForCondition` logic and its javadoc stay as the
   `waitForCondition` task left them.
6. **`String.format` → `.formatted`** wherever it survives (`FormsPage.openForm`), matching the
   entry's snippet and the rest of the test code.
7. **`FormsPage.toTitleCase`** — eleven lines of `StringBuilder` to turn `contact` into `Contact`.
   Its one caller is `openForm("contact")`, a single word. Replace with
   `key.substring(0, 1).toUpperCase() + key.substring(1)`.
   `shortcut: hyphenated keys now become "Two-words" rather than "Two Words"; restore the split if
   a test ever opens a form whose key has a hyphen.`

## Keep, deliberately

* The bounded waits behind `FormSubmissionPage.isSuccessMessageDisplayed` and
  `FormSubmissionsPage.isEmpty`. `isVisible()` does not wait, and the 2 s bound plus the `catch` is
  what makes a negative answer fast instead of a full-timeout failure. As `Locator` fields these
  become `successAlert.waitFor(new Locator.WaitForOptions().setTimeout(2000))` then
  `successAlert.isVisible()`, still returning `boolean`.
  Rejected alternative: `assertThat(successAlert).isVisible()` from
  `com.microsoft.playwright.assertions` (auto-retrying, same jar). It is the better Playwright
  style, but it turns these two methods from returning a boolean into throwing, which drags the
  `assertTrue`/`assertFalse` lines in both ITs into the change — including the `assertFalse` in
  `userAccessControlTest`, which wants a negative. Not worth it here.
* `waitForSelector(TABLE_ROWS)` in `isSubmissionPresent` — `Locator.count()` does not wait. It
  becomes `tableRows.first().waitFor()`, same guarantee.
* The public `waitForLoad()` methods. The ITs call them directly as page-transition markers
  (`FrontendIT:42,46,64,85,89,97,100,129`, `AdminRoleIT:42`); only the *internal* calls go.

## Not in this task

Migrating the selectors from CSS/XPath to `getByRole`/`getByLabel`/`getByTestId`. That is what
Playwright actually recommends, but it rewrites every selector against the real DOM, so it is a
different risk from a mechanical field swap. Own backlog entry if wanted.

## Verify

`mvn -pl backend -am verify` from the repo root — Docker must be running. The e2e ITs are the only
coverage these page objects have, and every line this task touches is on their path, so a lost wait
or a bad rewrite fails there. Expect `BUILD SUCCESS`. Run `verify` twice: the change is partly
about waiting, so a flake matters more than a pass. Playwright stays on 1.48.0 (see
`update-playwright`).

## Result

Five page objects, 82 insertions and 88 deletions, nothing outside
`backend/src/test/java/com/example/backend/e2e/pages/`. `BasePage` is untouched, no IT body
changed, and no selector text changed.

Done as planned:

* Deleted `FormSubmissionsPage.getSubmissionCount`, `FormSubmissionPage.isSubmitDisabled`,
  `FormSubmissionPage.cancel`, `FormsPage.openFirstForm`, and the `CANCEL_BUTTON` and
  `ERROR_ALERT` constants.
* Dropped eight redundant waits: five in `FormSubmissionPage`'s `fill*`/`select*`/`checkCheckbox`,
  the `waitForLoad()` in `getFormTitle`, the one in `LoginPage.login`, and the three internal
  `waitForLoad()` calls in `FormSubmissionsPage`'s first-row methods.
* Every surviving selector constant is now a `private final Locator` field. The five
  name-parameterized selectors are private methods returning a `Locator`, so each public method is
  one line: `textField(fieldName).fill(value)`.
* `page.waitForSelector(X)` became `x.waitFor()`, same default (visible) state. The two bounded
  waits became `waitFor(new Locator.WaitForOptions().setTimeout(2000))` and still return `boolean`,
  each with a one-line comment saying why the wait stays.
* `isSubmissionPresent`'s row wait is `tableRows.first().waitFor()`;
  `SubmissionDetailPage.fieldValueContains` is `detailsCard.locator("input, textarea")` instead of
  concatenating `.card-body` into two selectors.

Three things went differently:

* `fillTextArea`'s duplicated selector was already fixed in the IDE before I got there, so that
  part of the entry was done by hand.
* `toTitleCase` is `Character.toUpperCase(key.charAt(0)) + key.substring(1)`, not the
  `substring(0, 1).toUpperCase()` the plan wrote. Same result for the one caller and no default
  locale involved. The `shortcut:` note about hyphenated keys still applies, and is in the code.
* `FormsPage.openForm` keeps its internal `waitForLoad()`. The plan's step 2 did not list it, and
  it is not the same case as the others: it waits for the list to be on screen after the login
  redirect before locating a row in it.

No unit test added. These classes have no logic beyond the one-line `toTitleCase`, which both ITs
cover through `openForm("contact")` — a wrong title there fails the next assertion. No
documentation needed fixing: `backend/CLAUDE.md` only says page objects live in `e2e/pages/`.

## How to review

1. `git show HEAD -- backend/src/test/java/com/example/backend/e2e/pages/` is the whole change.
   Read `LoginPage` first — it is the pattern in nine lines — then `FormSubmissionPage`.
2. The claim that no selector changed: every string in the new `Locator` fields and the five
   private methods appears verbatim in the old constants. `git show HEAD -- ...FormSubmissionPage.java`
   side by side is enough to check it.
3. The claim that the four deleted methods were dead: `Grep` `getSubmissionCount|isSubmitDisabled|cancel\(|openFirstForm`
   over `backend/src` returns nothing.
4. `mvn -pl backend -am verify` from the repo root, Docker running. I ran it twice on this change:
   `BUILD SUCCESS` both times, `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` from failsafe
   (`AdminRoleIT` 1, `FrontendIT` 3), `FrontendIT` at 15.7 s and 15.7 s.
5. The judgement call worth a second opinion is in `## Result`: `openForm` keeps one wait the other
   methods lost.
