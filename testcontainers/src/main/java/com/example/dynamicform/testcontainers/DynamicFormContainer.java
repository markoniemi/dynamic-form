package com.example.dynamicform.testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs the dynamic-form backend in Docker. The image has no H2, so the caller supplies Postgres
 * and the auth server settings.
 */
public class DynamicFormContainer extends GenericContainer<DynamicFormContainer> {

  public static final int BACKEND_PORT = 8080;
  private static final String IMAGE_NAME = "ghcr.io/markoniemi/dynamic-form";

  /** Uses the image tagged with this jar's version. */
  public DynamicFormContainer() {
    this(DockerImageName.parse(IMAGE_NAME + ":" + version()));
  }

  /** Uses the given image, e.g. a pinned or local tag. */
  public DynamicFormContainer(DockerImageName image) {
    super(image);
    withExposedPorts(BACKEND_PORT);
    withEnv("SPRING_PROFILES_ACTIVE", "prod");
    waitingFor(
        Wait.forHttp("/actuator/health")
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofMinutes(2)));
  }

  /** Issuer the tokens carry; also returned by {@code /api/config/oauth2-issuer-uri}. */
  public DynamicFormContainer withIssuerUri(String uri) {
    return withEnv("OAUTH2_ISSUER_URI", uri)
        .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", uri);
  }

  /** Where the backend fetches signing keys, as reachable from inside the container. */
  public DynamicFormContainer withJwkSetUri(String uri) {
    return withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI", uri);
  }

  /** Postgres the backend uses (profile {@code prod}: Flyway migrations, sample forms seeded). */
  public DynamicFormContainer withDatabase(String jdbcUrl, String username, String password) {
    return withEnv("DB_URL", jdbcUrl).withEnv("DB_USERNAME", username).withEnv("DB_PASSWORD", password);
  }

  public String getBackendUrl() {
    return "http://localhost:" + getMappedPort(BACKEND_PORT);
  }

  private static String version() {
    try (InputStream in =
        DynamicFormContainer.class.getResourceAsStream("/dynamicform/version.properties")) {
      Properties properties = new Properties();
      properties.load(in);
      return properties.getProperty("version");
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read dynamic-form version", e);
    }
  }
}
