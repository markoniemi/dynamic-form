package com.example.backend.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class FormSubmissionPage extends BasePage {

  @FindBy(css = "h2.card-title")
  private WebElement formTitle;

  @FindBy(xpath = "//button[normalize-space()='Submit']")
  private WebElement submitButton;

  @FindBy(xpath = "//button[normalize-space()='Cancel']")
  private WebElement cancelButton;

  @FindBy(css = ".alert-success")
  private WebElement successAlert;

  @FindBy(css = ".alert-danger")
  private WebElement errorAlert;

  public FormSubmissionPage(WebDriver driver) {
    super(driver);
  }

  public void waitForLoad() {
    wait.until(ExpectedConditions.visibilityOf(formTitle));
  }

  public String getFormTitle() {
    waitForLoad();
    return formTitle.getText();
  }

  public void fillTextField(String fieldName, String value) {
    WebElement input = wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("input[name='" + fieldName + "']")));
    input.clear();
    input.sendKeys(value);
  }

  public void fillTextArea(String fieldName, String value) {
    WebElement area = wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("textarea[name='" + fieldName + "']")));
    area.clear();
    area.sendKeys(value);
  }

  public void selectOption(String fieldName, String visibleText) {
    WebElement selectEl = wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("select[name='" + fieldName + "']")));
    new Select(selectEl).selectByVisibleText(visibleText);
  }

  public void selectRadio(String fieldName, String value) {
    WebElement radio = wait.until(
        ExpectedConditions.elementToBeClickable(
            By.cssSelector("input[type='radio'][name='" + fieldName + "'][value='" + value + "']")));
    radio.click();
  }

  public void checkCheckbox(String fieldName, String value) {
    WebElement checkbox = wait.until(
        ExpectedConditions.elementToBeClickable(
            By.cssSelector("input[type='checkbox'][name='" + fieldName + "'][value='" + value + "']")));
    if (!checkbox.isSelected()) {
      checkbox.click();
    }
  }

  public void submit() {
    wait.until(ExpectedConditions.elementToBeClickable(submitButton));
    submitButton.click();
  }

  public boolean isSuccessMessageDisplayed() {
    try {
      wait.until(ExpectedConditions.visibilityOf(successAlert));
      return successAlert.isDisplayed();
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isSubmitDisabled() {
    return !submitButton.isEnabled()
        || Boolean.parseBoolean(submitButton.getAttribute("disabled"));
  }

  public void cancel() {
    wait.until(ExpectedConditions.elementToBeClickable(cancelButton));
    cancelButton.click();
  }
}
