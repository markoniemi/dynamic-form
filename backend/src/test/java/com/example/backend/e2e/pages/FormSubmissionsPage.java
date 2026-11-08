package com.example.backend.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class FormSubmissionsPage extends BasePage {
  private final Locator heading = page.locator("#submissions-heading");
  private final Locator emptyAlert = page.locator("#no-submissions");
  private final Locator tableRows = page.locator("#submissions-table tbody tr");
  private final Locator firstRowId =
      page.locator("#submissions-table tbody tr:first-child td:first-child");

  public FormSubmissionsPage(Page page) {
    super(page);
  }

  public void waitForLoad() {
    heading.waitFor();
  }

  public boolean isSubmissionPresent(String text) {
    waitForLoad();
    try {
      // count() reads the page as it is now, so the rows have to be there first
      tableRows.first().waitFor();
      int count = tableRows.count();
      for (int i = 0; i < count; i++) {
        if (tableRows.nth(i).textContent().contains(text)) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  public String getFirstSubmissionId() {
    return firstRowId.textContent();
  }

  public void viewSubmission(String submissionId) {
    page.locator("#view-%s".formatted(submissionId)).click();
  }

  public void editSubmission(String submissionId) {
    page.locator("#edit-%s".formatted(submissionId)).click();
  }

  // isVisible() never waits, so the alert needs a bounded wait of its own: a timeout here means
  // "not empty", which is an answer, not a failure.
  public boolean isEmpty() {
    try {
      emptyAlert.waitFor(new Locator.WaitForOptions().setTimeout(2000));
      return emptyAlert.isVisible();
    } catch (Exception e) {
      return false;
    }
  }
}
