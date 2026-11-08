package com.example.backend.e2e.pages;

import com.microsoft.playwright.Page;

public abstract class BasePage {
  protected Page page;

  public BasePage(Page page) {
    this.page = page;
  }
}
