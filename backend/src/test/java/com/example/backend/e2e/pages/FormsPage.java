package com.example.backend.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class FormsPage extends BasePage {
  private final Locator loginButton = page.locator("#login-button");
  private final Locator heading = page.locator("#forms-heading");
  private final Locator submissionsLink = page.locator("#nav-submissions");
  private final Locator logoutButton = page.locator("#logout-button");

  public FormsPage(Page page) {
    super(page);
  }

  public void clickLogin() {
    loginButton.click();
  }

  public void clickLogout() {
    logoutButton.click();
  }

  public void clickFormSubmissions() {
    submissionsLink.click();
  }

  public void waitForLoad() {
    heading.waitFor();
  }

  public void openForm(String formKey) {
    waitForLoad();
    page.locator("#open-%s".formatted(formKey)).click();
  }
}
