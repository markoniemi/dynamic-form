package com.example.backend.e2e.pages;

import com.microsoft.playwright.Page;

public class FormSubmissionPage extends BasePage {
  private static final String FORM_TITLE = "h2.card-title";
  private static final String SUBMIT_BUTTON = "button:has-text('Submit')";
  private static final String CANCEL_BUTTON = "button:has-text('Cancel')";
  private static final String SUCCESS_ALERT = ".alert-success";
  private static final String ERROR_ALERT = ".alert-danger";

  public FormSubmissionPage(Page page) {
    super(page);
  }

  public void waitForLoad() {
    page.waitForSelector(FORM_TITLE, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MS));
  }

  public String getFormTitle() {
    waitForLoad();
    return page.textContent(FORM_TITLE);
  }

  public void fillTextField(String fieldName, String value) {
    String selector = String.format("input[name='%s']", fieldName);
    page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MS));
    page.fill(selector, value);
  }

  public void fillTextArea(String fieldName, String value) {
    String selector = String.format("textarea[name='%s']", fieldName);
    page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MS));
    page.fill(selector, value);
  }

  public void selectOption(String fieldName, String visibleText) {
    String selector = String.format("select[name='%s']", fieldName);
    page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MS));
    page.selectOption(selector, visibleText);
  }

  public void selectRadio(String fieldName, String value) {
    String selector = String.format("input[type='radio'][name='%s'][value='%s']", fieldName, value);
    page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MS));
    page.click(selector);
  }

  public void checkCheckbox(String fieldName, String value) {
    String selector = String.format("input[type='checkbox'][name='%s'][value='%s']", fieldName, value);
    page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(TIMEOUT_MS));
    if (!page.locator(selector).isChecked()) {
      page.click(selector);
    }
  }

  public void submit() {
    page.click(SUBMIT_BUTTON);
  }

  public boolean isSuccessMessageDisplayed() {
    try {
      page.waitForSelector(SUCCESS_ALERT, new Page.WaitForSelectorOptions().setTimeout(2000));
      return page.locator(SUCCESS_ALERT).isVisible();
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isSubmitDisabled() {
    return page.locator(SUBMIT_BUTTON).isDisabled();
  }

  public void cancel() {
    page.click(CANCEL_BUTTON);
  }
}
