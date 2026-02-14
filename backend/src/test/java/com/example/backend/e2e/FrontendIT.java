package com.example.backend.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.backend.IntegrationTestBase;
import com.example.backend.e2e.pages.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;

public class FrontendIT extends IntegrationTestBase {

  @Autowired private WebDriver driver;
  private LoginPage loginPage;
  private FormsPage formsPage;
  private FormSubmissionPage formSubmissionPage;
  private FormSubmissionsPage formSubmissionsPage;
  private SubmissionDetailPage submissionDetailPage;

  @BeforeEach
  void setup() {
    loginPage = new LoginPage(driver);
    formsPage = new FormsPage(driver);
    formSubmissionPage = new FormSubmissionPage(driver);
    formSubmissionsPage = new FormSubmissionsPage(driver);
    submissionDetailPage = new SubmissionDetailPage(driver);
  }

  @Test
  void loginAndFormSubmission() {
    // 1. Navigate to Home Page and login
    driver.get("http://localhost:8080");
    // 2. Login
    formsPage.clickLogin();
    // 3. Login on Auth Server
    loginPage.login("admin", "admin");
    // 4. Navigate to the Forms list
    formsPage.waitForLoad();
    // 5. Open the "contact" form
    formsPage.openForm("contact");
    // 6. Verify the form page loaded
    formSubmissionPage.waitForLoad();
    assertTrue(formSubmissionPage.getFormTitle().contains("Contact"));
    // 7. Fill in the contact form fields
    formSubmissionPage.fillTextField("name", "Test User");
    formSubmissionPage.fillTextField("email", "test@example.com");
    formSubmissionPage.fillTextField("phone", "+1 555 000 0001");
    formSubmissionPage.selectOption("subject", "General Inquiry");
    formSubmissionPage.fillTextArea("message", "This is an automated e2e test message.");
    formSubmissionPage.selectRadio("urgency", "low");
    // 8. Submit the form
    formSubmissionPage.submit();
    // 9. Verify success message is shown
    assertTrue(formSubmissionPage.isSuccessMessageDisplayed());
    // 10. Navigate to Submissions page and verify the submission appears
    formsPage.clickFormSubmissions();
    assertTrue(formSubmissionsPage.isSubmissionPresent("contact"));
    // 11. View the submission details
    formSubmissionsPage.viewFirstSubmission();
    submissionDetailPage.waitForLoad();
    // 12. Verify submission details are correct
    assertTrue(submissionDetailPage.containsText("Test User"));
    assertTrue(submissionDetailPage.containsText("test@example.com"));
    assertTrue(submissionDetailPage.containsText("+1 555 000 0001"));
    assertTrue(submissionDetailPage.containsText("General Inquiry"));
    assertTrue(submissionDetailPage.containsText("This is an automated e2e test message."));
    assertTrue(submissionDetailPage.containsText("Low"));
    assertTrue(submissionDetailPage.containsText("admin"));
  }
}
