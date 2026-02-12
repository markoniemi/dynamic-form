package com.example.backend.e2e.pages;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ItemsPage extends BasePage {
  @FindBy(xpath = "//button[contains(text(), 'Login with OAuth2')]")
  private WebElement loginButton;

  @FindBy(xpath = "//button[contains(text(), 'Add Item')]")
  private WebElement addItemButton;

  @FindBy(className = "card")
  private List<WebElement> itemCards;

  @FindBy(xpath = "//button[contains(text(), 'Delete')]")
  private List<WebElement> deleteButtons;

  public ItemsPage(WebDriver driver) {
    super(driver);
  }

  public void clickLogin() {
    wait.until(ExpectedConditions.elementToBeClickable(loginButton));
    loginButton.click();
  }

  public void clickAddItem() {
    wait.until(ExpectedConditions.elementToBeClickable(addItemButton));
    addItemButton.click();
  }

  public boolean isItemPresent(String itemName) {
    wait.until(ExpectedConditions.presenceOfElementLocated(By.className("card")));
    return itemCards.stream().anyMatch(card -> card.getText().contains(itemName));
  }

  public void deleteItem(String itemName) {
    // This is a simplified approach. In a real scenario, you'd find the specific card and its
    // delete button.
    // For now, we'll just click the first delete button if available.
    wait.until(ExpectedConditions.presenceOfElementLocated(By.className("card")));
    if (!deleteButtons.isEmpty()) {
      deleteButtons.getFirst().click();
      driver.switchTo().alert().accept();
    }
  }

  public int getItemCount() {
    return itemCards.size();
  }
}
