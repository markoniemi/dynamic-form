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

  /**
   * True when the details card shows this text, either as visible text or as the value of one of
   * its fields. Retries until {@link #TIMEOUT_MS}, the way Playwright's own assertions do: the card
   * renders before the submission data resolves, and {@code Locator.count()} does not auto-wait, so
   * a single pass can run against fields that are not there yet and report a false negative.
   */
  public boolean containsText(String text) {
    waitForLoad();
    long deadline = System.currentTimeMillis() + TIMEOUT_MS;
    do {
      if (cardTextContains(text) || fieldValueContains(text)) {
        return true;
      }
      sleep(100);
    } while (System.currentTimeMillis() < deadline);
    return false;
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
