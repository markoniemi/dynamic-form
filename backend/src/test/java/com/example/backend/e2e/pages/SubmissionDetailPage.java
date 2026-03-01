package com.example.backend.e2e.pages;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class SubmissionDetailPage extends BasePage {

  @FindBy(xpath = "//h2")
  private WebElement heading;

  @FindBy(css = ".card-body")
  private WebElement detailsCard;

  public SubmissionDetailPage(WebDriver driver) {
    super(driver);
  }

  public void waitForLoad() {
    wait.until(ExpectedConditions.visibilityOf(heading));
  }

  public boolean isDetailPresent(String label, String value) {
    waitForLoad();
    // This method might need similar logic if used, but for now we focus on containsText
    return containsText(value);
  }

  public boolean containsText(String text) {
    waitForLoad();
    // Check visible text (labels, etc.)
    if (detailsCard.getText().contains(text)) {
      return true;
    }

    // Check input values (since ReadOnlyDynamicForm uses inputs for values)
    List<WebElement> inputs = detailsCard.findElements(By.cssSelector("input, textarea"));
    for (WebElement input : inputs) {
      String val = input.getAttribute("value");
      if (val != null && val.contains(text)) {
        return true;
      }
    }

    return false;
  }
}
