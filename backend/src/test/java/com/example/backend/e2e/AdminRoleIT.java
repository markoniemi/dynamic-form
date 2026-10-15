package com.example.backend.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.backend.IntegrationTestBase;
import com.example.backend.e2e.pages.FormsPage;
import com.example.backend.e2e.pages.LoginPage;
import com.microsoft.playwright.Page;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Logs in as the real auth server's {@code admin} and calls an admin-only endpoint with the issued
 * token, so a break in the claim-to-ROLE_ADMIN mapping fails here. Controller tests fake the
 * authorities and cannot catch that.
 */
public class AdminRoleIT extends IntegrationTestBase {

  private static final String FORM_JSON =
      """
      {"formKey":"admin-role-it","title":"Admin role IT",
       "fields":[{"name":"name","label":"Name","type":"text","required":true}]}
      """;

  private final Page page;

  @Autowired
  AdminRoleIT(Page page) {
    this.page = page;
  }

  @Test
  void adminTokenFromAuthServerCanCreateForm() throws Exception {
    page.navigate("http://localhost:8080");
    FormsPage formsPage = new FormsPage(page);
    formsPage.clickLogin();
    new LoginPage(page).login("admin", "admin");
    formsPage.waitForLoad();

    // oidc-client-ts keeps the signed-in user in sessionStorage
    String accessToken =
        (String)
            page.evaluate(
                "JSON.parse(sessionStorage.getItem(Object.keys(sessionStorage)"
                    + ".find(k => k.startsWith('oidc.user:')))).access_token");

    HttpResponse<String> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("http://localhost:8080/api/forms"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(FORM_JSON))
                    .build(),
                HttpResponse.BodyHandlers.ofString());

    assertEquals(201, response.statusCode(), response.body());
  }
}
