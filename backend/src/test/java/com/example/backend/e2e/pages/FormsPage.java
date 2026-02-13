package com.example.backend.e2e.pages;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class FormsPage extends BasePage {
  @FindBy(xpath = "//button[contains(text(), 'Login with OAuth2')]")
  private WebElement loginButton;

  @FindBy(xpath = "//h2[contains(text(), 'Available Forms')]")
  private WebElement heading;

  @FindBy(xpath = "//button[contains(text(), 'Open Form')]")
  private List<WebElement> openFormButtons;
  @FindBy(xpath = "//a[contains(text(), 'Submissions')]")
  private WebElement formSubmissionsLink;

  public FormsPage(WebDriver driver) {
    super(driver);
  }

  public void clickLogin() {
    wait.until(ExpectedConditions.elementToBeClickable(loginButton));
    loginButton.click();
  }
  public void clickFormSubmissions() {
    wait.until(ExpectedConditions.elementToBeClickable(formSubmissionsLink));
    formSubmissionsLink.click();
  }
  public void waitForLoad() {
    wait.until(ExpectedConditions.visibilityOf(heading));
  }

  public void openForm(String formKey) {
    waitForLoad();
    String formTitle = toTitleCase(formKey);
    openFormButtons.stream()
        .filter(btn -> btn.findElement(
            By.xpath("./ancestor::div[contains(@class,'card')]"))
            .getText().contains(formTitle))
        .findFirst()
        .orElseGet(() -> openFormButtons.getFirst())
        .click();
  }

  public void openFirstForm() {
    waitForLoad();
    wait.until(ExpectedConditions.elementToBeClickable(openFormButtons.getFirst()));
    openFormButtons.getFirst().click();
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
