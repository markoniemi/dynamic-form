package com.example.backend.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class FormSubmissionPage extends BasePage {
  private final Locator formTitle = page.locator("h2.card-title");
  private final Locator submitButton = page.locator("button:has-text('Submit')");
  private final Locator successAlert = page.locator(".alert-success");

  public FormSubmissionPage(Page page) {
    super(page);
  }

  public void waitForLoad() {
    formTitle.waitFor();
  }

  public String getFormTitle() {
    return formTitle.textContent();
  }

  public void fillTextField(String fieldName, String value) {
    textField(fieldName).fill(value);
  }

  public void fillTextArea(String fieldName, String value) {
    textArea(fieldName).fill(value);
  }

  public void selectOption(String fieldName, String visibleText) {
    select(fieldName).selectOption(visibleText);
  }

  public void selectRadio(String fieldName, String value) {
    radio(fieldName, value).click();
  }

  public void checkCheckbox(String fieldName, String value) {
    Locator box = checkbox(fieldName, value);
    if (!box.isChecked()) {
      box.click();
    }
  }

  public void submit() {
    submitButton.click();
  }

  // isVisible() never waits, so the alert needs a bounded wait of its own: a timeout here means
  // "not displayed", which is an answer, not a failure.
  public boolean isSuccessMessageDisplayed() {
    try {
      successAlert.waitFor(new Locator.WaitForOptions().setTimeout(2000));
      return successAlert.isVisible();
    } catch (Exception e) {
      return false;
    }
  }

  private Locator textField(String fieldName) {
    return page.locator("input[name='%s']".formatted(fieldName));
  }

  private Locator textArea(String fieldName) {
    return page.locator("textarea[name='%s']".formatted(fieldName));
  }

  private Locator select(String fieldName) {
    return page.locator("select[name='%s']".formatted(fieldName));
  }

  private Locator radio(String fieldName, String value) {
    return page.locator("input[type='radio'][name='%s'][value='%s']".formatted(fieldName, value));
  }

  private Locator checkbox(String fieldName, String value) {
    return page.locator(
        "input[type='checkbox'][name='%s'][value='%s']".formatted(fieldName, value));
  }
}
