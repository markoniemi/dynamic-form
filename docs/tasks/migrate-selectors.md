---
id: migrate-selectors
status: done
priority: medium
kind: task
---

Migrating the selectors from CSS/XPath to getByRole/getByLabel/getByTestId. That is what Playwright
actually recommends, but it rewrites every selector against the real DOM, so it is a different risk
from a mechanical field swap. Own backlog entry if wanted.

Re-planned after the first plan was rejected: **no selector may depend on button text or label
text.** Use ids and roles — `data-testid` rejected too, so every element an IT touches gets a real
`id`.

# Implementation Plan

Two naming corrections first, because the API does not have what the entry asks for: Playwright has
`getByRole`, `getByTestId`, `getByText`, `getByLabel`, `getByPlaceholder`, `getByAltText` and
`getByTitle` — there is no `getByClass` and no `getById`. An id or a class goes through
`page.locator("#id")` / `page.locator(".class")`, which is the supported way and is what this plan
means wherever it says "by id".

What the markup already gives us, verified:

* `FieldWrapper` passes `controlId={field.name}` to `Form.Group`, so react-bootstrap puts
  `id="<field.name>"` on the control — in the submission form **and** in `ReadOnlyDynamicForm` on
  the detail page.
* `RadioField` and `CheckboxField` set `id={`${field.name}-${option.value}`}` on each `Form.Check`.
* Nothing else has an id. `Navbar.Collapse` (`basic-navbar-nav`) and the language `NavDropdown`
  (`language-switcher`) are the only other ones, and no test wants them.

So the form fields need no frontend change at all, and everything else — buttons, headings, alerts,
table rows — has no id and must get a `data-testid`.

## Part 1 — fields by id, no frontend change, no IT change

This is the whole field surface, and the ITs keep passing field names exactly as they do today.

| Page object | Today | After |
| :- | :- | :- |
| `fillTextField` | `input[name='%s']` | `page.locator("#%s".formatted(fieldName))` |
| `fillTextArea` | `textarea[name='%s']` | same, by id |
| `selectOption` | `select[name='%s']` | same, by id |
| `selectRadio` | `input[type='radio'][name=…][value=…]` | `page.locator("#%s-%s".formatted(fieldName, value))` |
| `checkCheckbox` | `input[type='checkbox'][name=…][value=…]` | same, by id |
| `SubmissionDetailPage` field values | `.card-body input, .card-body textarea` loop | `page.locator("#%s".formatted(fieldName)).inputValue()` |

The last row is the real win: `fieldValueContains` loops every input in the card because it cannot
tell which field it is reading. By id it can, so `waitForText(text)` becomes
`waitForFieldValue(fieldName, value)` and both `cardTextContains` and `fieldValueContains` go away.
That changes the 7 `waitForText` call sites in `FrontendIT` to name the field they meant — the
field names are in `backend/src/main/resources/forms/contact.json`.

## Part 2 — the rest by `id`, which needs frontend edits

**This part edits `frontend/src/`, a second module.** Nothing outside the form fields has an id
today, so each element an IT touches gets one. All of them are hyphenated, and `EditForm.tsx:94`
validates field names as `^[a-zA-Z][a-zA-Z0-9]*$` — alphanumeric, no hyphens — so none of these
can ever collide with a field's control id.

An `id` on a react-bootstrap component passes straight through to the DOM element, so these are
one-attribute edits with no logic change; `npm run lint` and `npm test` in `frontend/` cover them.

**The one real constraint: an id must be unique in the document**, so a repeated element cannot
carry a fixed one. The three repeated cases take an id derived from their data instead, which ends
up stronger than what the page objects do today.

| Component | Element | `id` | Replaces |
| :- | :- | :- | :- |
| `pages/Login.tsx` | login button | `login-button` | `button:has-text('Login with OAuth2')` |
| `components/Navigation.tsx` | logout button | `logout-button` | `button:has-text('Logout')` |
| `components/Navigation.tsx` | submissions nav link | `nav-submissions` | `a:has-text('Submissions')` |
| `pages/Forms.tsx` | `h2` | `forms-heading` | `h2:has-text('Available Forms')` |
| `pages/Forms.tsx` | row `Button` | `` `open-${form.formKey}` `` | the `//td[contains(text(),…)]` XPath |
| `pages/FormSubmissions.tsx` | `h2` | `submissions-heading` | `h2:has-text('Form Submissions')` |
| `pages/FormSubmissions.tsx` | `Table` | `submissions-table` | `table tbody tr` (as the scope) |
| `pages/FormSubmissions.tsx` | view button | `` `view-${submission.id}` `` | `//table/tbody/tr[1]//button[…'View']` |
| `pages/FormSubmissions.tsx` | edit button | `` `edit-${submission.id}` `` | `//table/tbody/tr[1]//button[…'Edit']` |
| `pages/FormSubmissions.tsx` | empty `Alert` | `no-submissions` | `.alert-info` |
| `pages/FormSubmission.tsx` | `Card.Title` | `form-title` | `h2.card-title` |
| `pages/FormSubmission.tsx` | submit `Button` | `submit-button` | `button:has-text('Submit')` |
| `pages/FormSubmission.tsx` | success `Alert` | `submit-success` | `.alert-success` |
| `pages/SubmissionDetail.tsx` | metadata `Alert` | `submission-meta` | the `.card-body` text scrape |

What that does to the page objects:

* `FormsPage.openForm(formKey)` becomes `page.locator("#open-%s".formatted(formKey)).click()`. The
  signature the ITs already use stays, and **`toTitleCase` and its `shortcut:` comment are deleted**
  — no formKey-to-title guessing, no title text anywhere.
* `viewFirstSubmission()` / `editFirstSubmission()` become `viewSubmission(id)` /
  `editSubmission(id)` on `#view-<id>` / `#edit-<id>`, with the id from the existing
  `getFirstSubmissionId()` (`#submissions-table tbody tr:first-child td:first-child`). Three call
  sites in `FrontendIT` gain a line each — `String id = formSubmissionsPage.getFirstSubmissionId();`
  — and in exchange the test says which submission it opens instead of trusting row order.
* `isSubmissionPresent(text)` keeps scanning rows under `#submissions-table`, since it searches for
  a value rather than addressing an element.

## Part 3 — `getByRole` where it is honest

`getByRole` without a name matches every element of that role, so it is only usable where the role
is unique on the page: `getByRole(AriaRole.HEADING, setLevel(2))` for the one `h2`, and
`getByRole(AriaRole.ALERT)` for a page showing exactly one alert. Everywhere else a role needs a
*name* to be unique, and a name is the visible text this task is removing. So roles are the
fallback here, not the main tool — which is why Part 2 carries the weight.

With Part 2 done, every element in the table above has an id, so `getByRole` ends up used nowhere
and the page objects read as ids throughout. That is the plan, not an oversight: an id beats a role
plus a guess about uniqueness.

`LoginPage` keeps its three `input[name=…]` selectors: that page is served by
`../../oauth2-server`, so neither an id nor a `data-testid` is ours to add.

## Verify

* `frontend/`: `npm run lint` and `npm test` for the `data-testid` edits.
* repo root: `mvn -pl backend -am verify`, Docker running. Expect `BUILD SUCCESS` and
  `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` from failsafe. Run it twice — every line of
  this task is a selector, and a selector that matches the wrong element can pass once.
* The default timeout is 10 s (`PlaywrightConfig.DEFAULT_TIMEOUT_MS`), so a selector that matches
  nothing costs 10 s before it fails. Slow failure, not a silent pass.

## Result

Parts 1 and 2, both implemented. 11 files, 70 insertions and 99 deletions: 14 `id` attributes
across 6 frontend files (the plan said 5 — `SubmissionDetail.tsx` was missing from that count),
four page objects, and `FrontendIT`.

**Not one selector in the page objects depends on visible text any more.** Every one is an id,
except the three in `LoginPage`, which belong to `../../oauth2-server` and were left alone as
planned.

What the ids bought, beyond the swap:

* `FormsPage.openForm("contact")` is `#open-contact`. **`toTitleCase` and its `shortcut:` comment
  are gone** — the formKey-to-title guess that the previous task had reduced to one line is now
  deleted outright, along with the private `openFormButton` helper.
* `SubmissionDetailPage` lost `cardTextContains`, `fieldValueContains` and the `.card-body` locator.
  `waitForText(text)`, which scanned the whole card and every input in it, is now
  `waitForFieldValue(fieldName, value)` on `#<fieldName>` and `waitForMetadata(text)` on
  `#submission-meta`. The 7 call sites in `FrontendIT` now say which field each line checks, and
  the field assertions compare the whole value with `equals` instead of `contains`.
* `viewFirstSubmission`/`editFirstSubmission` are `viewSubmission(id)`/`editSubmission(id)` on
  `#view-<id>`/`#edit-<id>`, fed by the existing `getFirstSubmissionId()`. The tests no longer
  trust row order to reach a submission.
* `fillTextField`, `fillTextArea` and `selectOption` now share one private `field(fieldName)`
  helper — `#<fieldName>` is the same id whatever the control is — and radio and checkbox share
  `option(fieldName, value)` on `#<fieldName>-<value>`. Five private methods became two.
* `FrontendIT`'s own calls are otherwise unchanged: it still passes field names and the form key,
  not labels or titles.

Two deviations from the plan:

* `SubmissionDetailPage.waitForLoad()` waits for `#submission-meta`, not the `h2`. The plan's table
  gave no id to that heading, and the metadata alert is a better load marker anyway: it is part of
  the submission, so it cannot be on screen before the data is. No new id was needed.
* `isSubmissionPresent` keeps scanning rows, now under `#submissions-table tbody tr`. It searches
  for a value rather than addressing an element, so there is nothing to give an id to.

Part 3 is therefore moot: with every element addressed by id, `getByRole` is used nowhere. That is
the outcome the plan predicted, not an omission.

## How to review

1. `git show HEAD --stat` first, then the frontend half:
   `git show HEAD -- frontend/src`. Fourteen one-attribute additions, no logic touched. The three
   data-derived ones (`open-${form.formKey}`, `view-${submission.id}`, `edit-${submission.id}`) are
   the only ones worth a second look, since a fixed id on a repeated row would be invalid HTML.
2. `git show HEAD -- backend/src/test/.../pages/` for the page objects. `FormsPage` is the clearest
   case: 39 lines to 35, with `toTitleCase` and the XPath gone.
3. The claim that no page object reads visible text: `Grep` `has-text|contains\(text` over
   `backend/src/test` returns nothing.
4. `cd frontend; npm run lint; npm test` — I ran both: lint silent, `Test Files 8 passed (8)`,
   `Tests 53 passed (53)`.
5. `mvn -pl backend -am verify` from the repo root, Docker running. I ran it twice after the change:
   `BUILD SUCCESS` both times, `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` from failsafe,
   `FrontendIT` 15.6 s both runs.
6. The judgement call worth your eye is in `## Result`: `waitForLoad` on the detail page now waits
   for the metadata alert rather than the heading.
