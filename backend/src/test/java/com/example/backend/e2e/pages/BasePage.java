package com.example.backend.e2e.pages;

import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BasePage {
  protected static final int TIMEOUT_MS = 10000;
  protected Page page;

  public BasePage(Page page) {
    this.page = page;
  }

  protected void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      log.warn(e.getMessage(), e);
    }
  }
}
