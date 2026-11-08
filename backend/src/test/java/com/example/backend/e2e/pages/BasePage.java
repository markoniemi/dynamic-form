package com.example.backend.e2e.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import java.util.function.BooleanSupplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BasePage {
  protected static final int TIMEOUT_MS = 10000;
  protected Page page;

  public BasePage(Page page) {
    this.page = page;
  }

  /**
   * Blocks until the condition holds, up to {@link #TIMEOUT_MS}, and reports whether it did. Use
   * this for any check built from calls that do not auto-wait themselves — {@code Locator.count()}
   * and {@code Locator.inputValue()} read the page as it is right now, so a single pass can run
   * before the data has rendered and report a false negative.
   *
   * <p>Playwright dispatches its events while this waits, which a {@link #sleep} loop does not.
   */
  protected boolean waitUntil(BooleanSupplier condition) {
    try {
      page.waitForCondition(condition, new Page.WaitForConditionOptions().setTimeout(TIMEOUT_MS));
      return true;
    } catch (TimeoutError e) {
      return false;
    }
  }

  protected void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      log.warn(e.getMessage(), e);
    }
  }
}
