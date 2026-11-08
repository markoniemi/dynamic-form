package com.example.backend.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class FormSubmissionsPage extends BasePage {
  private final Locator heading = page.locator("h2:has-text('Form Submissions')");
  private final Locator emptyAlert = page.locator(".alert-info");
  private final Locator tableRows = page.locator("table tbody tr");
  private final Locator firstRowId = page.locator("//table/tbody/tr[1]/td[1]");
  private final Locator firstRowViewButton =
      page.locator("//table/tbody/tr[1]//button[contains(text(), 'View')]");
  private final Locator firstRowEditButton =
      page.locator("//table/tbody/tr[1]//button[contains(text(), 'Edit')]");

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

  public void viewFirstSubmission() {
    firstRowViewButton.click();
  }

  public void editFirstSubmission() {
    firstRowEditButton.click();
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
