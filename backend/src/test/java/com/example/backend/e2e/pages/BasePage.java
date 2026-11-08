package com.example.backend.e2e.pages;

import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BasePage {
  protected static final int TIMEOUT_MS = 10000;
  protected Page page;

  public BasePage(Page page) {
    this.page = page;
    // Every waiting call without explicit options uses this, waitForCondition included. All page
    // objects share one Page and set the same value, so repeating it is harmless.
    page.setDefaultTimeout(TIMEOUT_MS);
  }

  protected void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      log.warn(e.getMessage(), e);
    }
  }
}
