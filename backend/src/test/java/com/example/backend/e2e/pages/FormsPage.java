package com.example.backend.e2e.pages;

import com.microsoft.playwright.Page;

public class FormsPage extends BasePage {
  private static final String LOGIN_BUTTON = "button:has-text('Login with OAuth2')";
  private static final String HEADING = "h2:has-text('Available Forms')";
  private static final String SUBMISSIONS_LINK = "a:has-text('Submissions')";
  private static final String LOGOUT_BUTTON = "button:has-text('Logout')";

  public FormsPage(Page page) {
    super(page);
  }

  public void clickLogin() {
    page.click(LOGIN_BUTTON);
  }

  public void clickLogout() {
    page.click(LOGOUT_BUTTON);
  }

  public void clickFormSubmissions() {
    page.click(SUBMISSIONS_LINK);
  }

  public void waitForLoad() {
    page.waitForSelector(HEADING, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MS));
  }

  public void openForm(String formKey) {
    waitForLoad();
    String formTitle = toTitleCase(formKey);
    String xpath = String.format("//td[contains(text(), '%s')]/..//button[contains(text(), 'Open Form')]", formTitle);
    page.click(xpath);
  }

  public void openFirstForm() {
    waitForLoad();
    page.click("//tbody/tr[1]//button[contains(text(), 'Open Form')]");
  }

  private String toTitleCase(String key) {
    String[] parts = key.split("-");
    StringBuilder sb = new StringBuilder();
    for (String part : parts) {
      if (!part.isEmpty()) {
        sb.append(Character.toUpperCase(part.charAt(0)))
            .append(part.substring(1).toLowerCase())
            .append(" ");
      }
    }
    return sb.toString().trim();
  }
}
