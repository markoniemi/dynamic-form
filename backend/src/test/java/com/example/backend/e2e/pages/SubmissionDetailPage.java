package com.example.backend.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class SubmissionDetailPage extends BasePage {
  private final Locator metadata = page.locator("#submission-meta");

  public SubmissionDetailPage(Page page) {
    super(page);
  }

  public void waitForLoad() {
    metadata.waitFor();
  }

  /**
   * Waits until this field shows this value, and throws {@link
   * com.microsoft.playwright.TimeoutError} if it never does.
   *
   * <p>The card renders before the submission data resolves, so this has to wait rather than read
   * once: {@code Locator.inputValue()} reports the page as it is right now.
   * {@code waitForCondition} dispatches Playwright events while it polls, which a
   * {@code Thread.sleep} loop does not.
   *
   * <p>ReadOnlyDynamicForm renders each value into a control that carries the field name as its id,
   * so the value is addressed rather than searched for. Select and radio fields show the option
   * label, not the stored value.
   */
  public void waitForFieldValue(String fieldName, String value) {
    Locator field = page.locator("#%s".formatted(fieldName));
    page.waitForCondition(() -> value.equals(field.inputValue()));
  }

  /** Waits for text in the submission metadata: its id, form key, timestamp or submitter. */
  public void waitForMetadata(String text) {
    page.waitForCondition(() -> metadata.textContent().contains(text));
  }
}
