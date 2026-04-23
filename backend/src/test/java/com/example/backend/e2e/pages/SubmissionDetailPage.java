package com.example.backend.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class SubmissionDetailPage extends BasePage {
  private static final String HEADING = "//h2";
  private static final String DETAILS_CARD = ".card-body";

  public SubmissionDetailPage(Page page) {
    super(page);
  }

  public void waitForLoad() {
    page.waitForSelector(HEADING, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MS));
  }

  public boolean isDetailPresent(String label, String value) {
    waitForLoad();
    return containsText(value);
  }

  public boolean containsText(String text) {
    waitForLoad();
    // Check visible text (labels, etc.)
    if (page.locator(DETAILS_CARD).textContent().contains(text)) {
      return true;
    }

    // Check input values (since ReadOnlyDynamicForm uses inputs for values)
    Locator inputs = page.locator(DETAILS_CARD + " input, " + DETAILS_CARD + " textarea");
    int count = inputs.count();
    for (int i = 0; i < count; i++) {
      String val = inputs.nth(i).inputValue();
      if (val != null && val.contains(text)) {
        return true;
      }
    }

    return false;
  }
}
