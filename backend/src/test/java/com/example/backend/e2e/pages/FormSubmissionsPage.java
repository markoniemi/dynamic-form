package com.example.backend.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class FormSubmissionsPage extends BasePage {

  @FindBy(xpath = "//h2[contains(text(), 'Form Submissions')]")
  private WebElement heading;

  @FindBy(css = ".alert-info")
  private WebElement emptyAlert;

  public FormSubmissionsPage(WebDriver driver) {
    super(driver);
  }

  public void waitForLoad() {
    wait.until(ExpectedConditions.visibilityOf(heading));
  }

  public boolean isSubmissionPresent(String text) {
    waitForLoad();
    try {
      wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table tbody tr")));
      return driver.findElements(By.cssSelector("table tbody tr"))
          .stream().anyMatch(row -> row.getText().contains(text));
    } catch (Exception e) {
      return false;
    }
  }

  public String getFirstSubmissionId() {
    waitForLoad();
    WebElement firstCell = wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.xpath("//table/tbody/tr[1]/td[1]")));
    return firstCell.getText();
  }

  public void viewFirstSubmission() {
    waitForLoad();
    WebElement viewButton = wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//table/tbody/tr[1]//button[contains(text(), 'View')]")));
    viewButton.click();
  }

  public void editFirstSubmission() {
    waitForLoad();
    WebElement editButton = wait.until(ExpectedConditions.elementToBeClickable(
        By.xpath("//table/tbody/tr[1]//button[contains(text(), 'Edit')]")));
    editButton.click();
  }

  public int getSubmissionCount() {
    waitForLoad();
    return driver.findElements(By.cssSelector("table tbody tr")).size();
  }

  public boolean isEmpty() {
    try {
      wait.until(ExpectedConditions.visibilityOf(emptyAlert));
      return emptyAlert.isDisplayed();
    } catch (Exception e) {
      return false;
    }
  }
}
