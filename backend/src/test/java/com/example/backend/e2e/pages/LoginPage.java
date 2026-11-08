package com.example.backend.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class LoginPage extends BasePage {
  private final Locator usernameInput = page.locator("input[name='username']");
  private final Locator passwordInput = page.locator("input[name='password']");
  private final Locator loginButton = page.locator("button[type='submit']");

  public LoginPage(Page page) {
    super(page);
  }

  public void login(String username, String password) {
    usernameInput.fill(username);
    passwordInput.fill(password);
    loginButton.click();
  }
}
