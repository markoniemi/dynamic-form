package com.example.backend.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class FormSubmissionPage extends BasePage {
  private final Locator formTitle = page.locator("#form-title");
  private final Locator submitButton = page.locator("#submit-button");
  private final Locator successAlert = page.locator("#submit-success");

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
    field(fieldName).fill(value);
  }

  public void fillTextArea(String fieldName, String value) {
    field(fieldName).fill(value);
  }

  public void selectOption(String fieldName, String visibleText) {
    field(fieldName).selectOption(visibleText);
  }

  public void selectRadio(String fieldName, String value) {
    option(fieldName, value).click();
  }

  public void checkCheckbox(String fieldName, String value) {
    Locator box = option(fieldName, value);
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

  // FieldWrapper passes controlId={field.name}, so every text, textarea and select control carries
  // the field name as its id
  private Locator field(String fieldName) {
    return page.locator("#%s".formatted(fieldName));
  }

  // RadioField and CheckboxField give each option the id <field name>-<option value>
  private Locator option(String fieldName, String value) {
    return page.locator("#%s-%s".formatted(fieldName, value));
  }
}
