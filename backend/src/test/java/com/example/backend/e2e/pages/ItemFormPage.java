package com.example.backend.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ItemFormPage extends BasePage {
  @FindBy(name = "name")
  private WebElement nameInput;

  @FindBy(name = "description")
  private WebElement descriptionInput;

  @FindBy(xpath = "//button[contains(text(), 'Save Item')]")
  private WebElement saveButton;

  public ItemFormPage(WebDriver driver) {
    super(driver);
  }

  public void fillForm(String name, String description) {
    wait.until(ExpectedConditions.visibilityOf(nameInput));
    nameInput.clear();
    nameInput.sendKeys(name);
    descriptionInput.clear();
    descriptionInput.sendKeys(description);
  }

  public void submit() {
    saveButton.click();
    wait.until(ExpectedConditions.presenceOfElementLocated(By.className("card")));
  }
}
