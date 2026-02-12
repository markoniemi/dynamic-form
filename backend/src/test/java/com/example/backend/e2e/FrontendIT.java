package com.example.backend.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.backend.IntegrationTestBase;
import com.example.backend.e2e.pages.ItemFormPage;
import com.example.backend.e2e.pages.ItemsPage;
import com.example.backend.e2e.pages.LoginPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;

public class FrontendIT extends IntegrationTestBase {
  @Autowired
  private WebDriver driver;
  private ItemsPage homePage;
  private LoginPage loginPage;
  private ItemFormPage itemFormPage;
  @BeforeEach
  void setup() {
    homePage = new ItemsPage(driver);
    loginPage = new LoginPage(driver);
    itemFormPage = new ItemFormPage(driver);
  }
  @Test
  void loginAndItemManagement() {
    // 1. Navigate to Home Page
    driver.get("http://localhost:8080");
    // 2. Login
    homePage.clickLogin();
    // 3. Login on Auth Server
    loginPage.login("admin", "admin");
    // 4. Add Item
    homePage.clickAddItem();
    itemFormPage.fillForm("Test Item", "This is a test item");
    itemFormPage.submit();
    // 5. Verify Item Added
    assertTrue(homePage.isItemPresent("Test Item"), "Item should be present after addition");
    // 6. Delete Item (Cleanup)
    homePage.deleteItem("Test Item");
  }
}
