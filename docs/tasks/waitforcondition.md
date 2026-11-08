---
id: waitforcondition
status: in_progress
priority: low
kind: task
---

- remove BasePage.sleep

Set to `planned` with no plan section, which the protocol reads as a waiver.

The `waitForCondition` half of the original entry landed in `update-playwright`:
`SubmissionDetailPage.waitForText` calls `Page.waitForCondition` directly. What
is left is the deletion.
