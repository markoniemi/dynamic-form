---
id: waitforcondition
status: in_review
priority: low
kind: task
---

- remove BasePage.sleep

Set to `planned` with no plan section, which the protocol reads as a waiver.

The `waitForCondition` half of the original entry landed in `update-playwright`:
`SubmissionDetailPage.waitForText` calls `Page.waitForCondition` directly. What
is left is the deletion.

## Result

`BasePage.sleep` had no caller — a grep of `backend/src` for `sleep(` found only
its own declaration and body. So nothing had to be migrated to
`waitForCondition`; the method was already dead when this task opened.

Deleted with it: `@Slf4j` and its import, since `log` was used nowhere but that
method's `catch`. `BasePage` is now nine lines — the `page` field and the
constructor.

One reference needed fixing: `SubmissionDetailPage.waitForText`'s javadoc said
"which a `{@link #sleep}` loop does not", which would have pointed at a member
that no longer exists. Now `{@code Thread.sleep}`, which is what it always meant.

Left alone: `FormDataRepositoryTest:41` calls `Thread.sleep(10)` directly. It is
not a page object and never used this helper — it orders two timestamps, which is
not something `waitForCondition` can do.

## How to review

Nothing is pushed, so this is the commits on `master`.

1. `git show HEAD~1 -- backend/src/test/java/com/example/backend/e2e/pages/BasePage.java`
   is the whole change. The claim to check is that nothing called it:
   `Grep` `sleep\(` over `backend/src` should return only
   `FormDataRepositoryTest:41`.
2. `mvn -pl backend -am verify` from the repo root. Expect `BUILD SUCCESS` and
   `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`. Run once — this change
   cannot be intermittent, since it deletes unreachable code.
