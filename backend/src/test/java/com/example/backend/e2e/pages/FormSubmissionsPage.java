package com.example.backend.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.List;

public class FormSubmissionsPage extends BasePage {
  private static final String HEADING = "h2:has-text('Form Submissions')";
  private static final String EMPTY_ALERT = ".alert-info";
  private static final String TABLE_ROWS = "table tbody tr";

  public FormSubmissionsPage(Page page) {
    super(page);
  }

  public void waitForLoad() {
    page.waitForSelector(HEADING, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MS));
  }

  public boolean isSubmissionPresent(String text) {
    waitForLoad();
    try {
      page.waitForSelector(TABLE_ROWS, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MS));
      Locator rows = page.locator(TABLE_ROWS);
      int count = rows.count();
      for (int i = 0; i < count; i++) {
        if (rows.nth(i).textContent().contains(text)) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  public String getFirstSubmissionId() {
    waitForLoad();
    return page.textContent("//table/tbody/tr[1]/td[1]");
  }

  public void viewFirstSubmission() {
    waitForLoad();
    page.click("//table/tbody/tr[1]//button:has-text('View')");
  }

  public void editFirstSubmission() {
    waitForLoad();
    page.click("//table/tbody/tr[1]//button:has-text('Edit')");
  }

  public int getSubmissionCount() {
    waitForLoad();
    return page.locator(TABLE_ROWS).count();
  }

  public boolean isEmpty() {
    try {
      page.waitForSelector(EMPTY_ALERT, new Page.WaitForSelectorOptions().setTimeout(2000));
      return page.locator(EMPTY_ALERT).isVisible();
    } catch (Exception e) {
      return false;
    }
  }
}
