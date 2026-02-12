package com.example.backend.e2e.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LoginPage extends BasePage {
  @FindBy(name = "username")
  private WebElement usernameInput;

  @FindBy(name = "password")
  private WebElement passwordInput;

  @FindBy(css = "button[type='submit']")
  private WebElement loginButton;

  public LoginPage(WebDriver driver) {
    super(driver);
  }

  public void login(String username, String password) {
    wait.until(ExpectedConditions.visibilityOf(usernameInput));
    usernameInput.sendKeys(username);
    passwordInput.sendKeys(password);
    loginButton.click();
  }
}
