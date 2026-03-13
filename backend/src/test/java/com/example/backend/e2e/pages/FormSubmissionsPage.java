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

  public boolean isSubmissionPresent(String text) {
    waitForLoad();
    try {
      wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table tbody tr")));
      return submissionRows.stream().anyMatch(row -> row.getText().contains(text));
    } catch (Exception e) {
      return false;
    }
  }

  public String getFirstSubmissionId() {
    waitForLoad();
    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table tbody tr")));
    if (!submissionRows.isEmpty()) {
      return submissionRows.getFirst().findElement(By.xpath("./td[1]")).getText();
    }
    return null;
  }

  public void viewFirstSubmission() {
    waitForLoad();
    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table tbody tr")));
    if (!submissionRows.isEmpty()) {
      WebElement firstRow = submissionRows.getFirst();
      WebElement viewButton = firstRow.findElement(By.xpath(".//button[contains(text(), 'View')]"));
      viewButton.click();
    }
  }

  public void editFirstSubmission() {
    waitForLoad();
    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table tbody tr")));
    if (!submissionRows.isEmpty()) {
      WebElement firstRow = submissionRows.getFirst();
      WebElement editButton = firstRow.findElement(By.xpath(".//button[contains(text(), 'Edit')]"));
      editButton.click();
    }
  }

  public int getSubmissionCount() {
    waitForLoad();
    return submissionRows.size();
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
