package com.example.backend.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class FormsPage extends BasePage {
  private final Locator loginButton = page.locator("button:has-text('Login with OAuth2')");
  private final Locator heading = page.locator("h2:has-text('Available Forms')");
  private final Locator submissionsLink = page.locator("a:has-text('Submissions')");
  private final Locator logoutButton = page.locator("button:has-text('Logout')");

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
    openFormButton(toTitleCase(formKey)).click();
  }

  private Locator openFormButton(String formTitle) {
    return page.locator(
        "//td[contains(text(), '%s')]/..//button[contains(text(), 'Open Form')]"
            .formatted(formTitle));
  }

  private String toTitleCase(String key) {
    return Character.toUpperCase(key.charAt(0)) + key.substring(1);
  }
}
