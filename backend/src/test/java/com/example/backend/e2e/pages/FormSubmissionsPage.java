package com.example.backend.e2e.pages;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class FormSubmissionsPage extends BasePage {

  @FindBy(xpath = "//h2[contains(text(), 'Form Submissions')]")
  private WebElement heading;

  @FindBy(css = "table tbody tr")
  private List<WebElement> submissionRows;

  @FindBy(css = ".alert-info")
  private WebElement emptyAlert;

  public FormSubmissionsPage(WebDriver driver) {
    super(driver);
  }

  public void waitForLoad() {
    wait.until(ExpectedConditions.visibilityOf(heading));
  }

  public boolean isSubmissionPresent(String formKey) {
    waitForLoad();
    try {
      wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table tbody tr")));
      return submissionRows.stream().anyMatch(row -> row.getText().contains(formKey));
    } catch (Exception e) {
      return false;
    }
  }

  public int getSubmissionCount() {
    waitForLoad();
    return submissionRows.size();
  }

  public boolean isEmpty() {
    try {
      return emptyAlert.isDisplayed();
    } catch (Exception e) {
      return false;
    }
  }
}
