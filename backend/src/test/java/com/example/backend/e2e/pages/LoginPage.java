package com.example.backend.e2e.pages;

import com.microsoft.playwright.Page;

public class LoginPage extends BasePage {
  private static final String USERNAME_INPUT = "input[name='username']";
  private static final String PASSWORD_INPUT = "input[name='password']";
  private static final String LOGIN_BUTTON = "button[type='submit']";

  public LoginPage(Page page) {
    super(page);
  }

  public void login(String username, String password) {
    page.waitForSelector(USERNAME_INPUT, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MS));
    page.fill(USERNAME_INPUT, username);
    page.fill(PASSWORD_INPUT, password);
    page.click(LOGIN_BUTTON);
  }
}
