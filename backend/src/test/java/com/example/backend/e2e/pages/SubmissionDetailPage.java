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
    page.waitForSelector(HEADING);
  }

  /**
   * Waits until the details card shows this text, either as visible text or as the value of one of
   * its fields, and throws {@link com.microsoft.playwright.TimeoutError} if it never does.
   *
   * <p>The card renders before the submission data resolves, so this has to wait rather than read
   * once: {@code Locator.count()} and {@code Locator.inputValue()} report the page as it is right
   * now. {@code waitForCondition} dispatches Playwright events while it polls, which a
   * {@code Thread.sleep} loop does not.
   */
  public void waitForText(String text) {
    waitForLoad();
    page.waitForCondition(() -> cardTextContains(text) || fieldValueContains(text));
  }

  private boolean cardTextContains(String text) {
    String cardText = page.locator(DETAILS_CARD).first().textContent();
    return cardText != null && cardText.contains(text);
  }

  // ReadOnlyDynamicForm renders values into inputs, so they are not in textContent
  private boolean fieldValueContains(String text) {
    Locator fields = page.locator(DETAILS_CARD + " input, " + DETAILS_CARD + " textarea");
    int count = fields.count();
    for (int i = 0; i < count; i++) {
      String value = fields.nth(i).inputValue();
      if (value != null && value.contains(text)) {
        return true;
      }
    }
    return false;
  }
}
