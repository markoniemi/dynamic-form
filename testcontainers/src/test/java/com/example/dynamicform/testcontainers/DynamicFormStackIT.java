package com.example.dynamicform.testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DynamicFormStackIT {

  private static final String VERIFIER = "test-verifier-0123456789-0123456789-0123456789";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static DynamicFormStack stack;

  @BeforeAll
  static void start() {
    stack =
        new DynamicFormStack()
            .withAuthConfigFile("desktop-client.yaml")
            .withLogConsumer(frame -> System.out.print(frame.getUtf8String()))
            .start();
  }

  @AfterAll
  static void stop() {
    if (stack != null) {
      stack.close();
    }
  }

  @Test
  void backendReturnsAuthServerIssuer() throws Exception {
    HttpResponse<String> response = get(stack.backendUrl() + "/api/config/oauth2-issuer-uri", null);

    assertEquals(200, response.statusCode());
    assertEquals("http://localhost:9000", response.body());
  }

  @Test
  void adminReadsSeededForms() throws Exception {
    HttpResponse<String> response =
        get(stack.backendUrl() + "/api/forms", token("frontend-client", "http://localhost:8080", "admin", "admin"));

    assertEquals(200, response.statusCode());
    assertTrue(MAPPER.readTree(response.body()).size() > 0, "stack should seed sample forms");
  }

  @Test
  void userCannotCreateForm() throws Exception {
    String token = token("frontend-client", "http://localhost:8080", "user", "user");

    assertEquals(200, get(stack.backendUrl() + "/api/forms", token).statusCode());
    String validForm =
        "{\"formKey\":\"it-form\",\"title\":\"IT\",\"fields\":"
            + "[{\"name\":\"a\",\"label\":\"A\",\"type\":\"text\"}]}";
    assertEquals(403, post(stack.backendUrl() + "/api/forms", token, validForm).statusCode());
  }

  @Test
  void extraConfigAddsClientAndKeepsDefaultUsers() throws Exception {
    String token = token("desktop-client", "http://localhost:8123/callback", "admin", "admin");

    assertEquals(200, get(stack.backendUrl() + "/api/forms", token).statusCode());
  }

  // Authorization code flow with PKCE, driven by hand: login form, then code exchange.
  private static String token(String clientId, String redirectUri, String username, String password)
      throws Exception {
    HttpClient client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    String authorize =
        stack.authUrl()
            + "/oauth2/authorize?response_type=code&scope=openid&code_challenge_method=S256"
            + "&client_id=" + clientId
            + "&redirect_uri=" + redirectUri
            + "&code_challenge=" + challenge();

    URI login = redirect(client, HttpRequest.newBuilder(URI.create(authorize)).build());
    String page = client.send(HttpRequest.newBuilder(login).build(), HttpResponse.BodyHandlers.ofString()).body();
    Matcher csrf = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page);
    assertTrue(csrf.find(), "login page has no csrf field for " + clientId);

    URI resume =
        redirect(
            client,
            HttpRequest.newBuilder(login)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(form(Map.of("username", username, "password", password, "_csrf", csrf.group(1))))
                .build());
    URI callback = redirect(client, HttpRequest.newBuilder(resume).build());
    Matcher code = Pattern.compile("[?&]code=([^&]+)").matcher(callback.toString());
    assertTrue(code.find(), "no code in " + callback);

    HttpResponse<String> tokens =
        client.send(
            HttpRequest.newBuilder(URI.create(stack.authUrl() + "/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(
                    form(
                        Map.of(
                            "grant_type", "authorization_code",
                            "code", code.group(1),
                            "redirect_uri", redirectUri,
                            "client_id", clientId,
                            "code_verifier", VERIFIER)))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, tokens.statusCode(), tokens.body());
    JsonNode json = MAPPER.readTree(tokens.body());
    assertFalse(json.path("access_token").asText().isEmpty());
    return json.get("access_token").asText();
  }

  /** Sends the request, expects a redirect, and returns its target resolved against the request. */
  private static URI redirect(HttpClient client, HttpRequest request) throws Exception {
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(302, response.statusCode(), request.uri() + " -> " + response.body());
    return request.uri().resolve(response.headers().firstValue("Location").orElseThrow());
  }

  private static HttpRequest.BodyPublisher form(Map<String, String> fields) {
    return HttpRequest.BodyPublishers.ofString(
        fields.entrySet().stream()
            .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&")));
  }

  private static String challenge() throws Exception {
    byte[] hash = MessageDigest.getInstance("SHA-256").digest(VERIFIER.getBytes(StandardCharsets.US_ASCII));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
  }

  private static HttpResponse<String> get(String url, String token) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url));
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static HttpResponse<String> post(String url, String token, String json) throws Exception {
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }
}
