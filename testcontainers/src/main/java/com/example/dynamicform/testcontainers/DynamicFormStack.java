package com.example.dynamicform.testcontainers;

import com.example.auth.testcontainers.OAuth2Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import java.util.function.Consumer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Auth server and backend on one network, wired like {@code docker-compose.yaml}. The auth server
 * is bound to host port 9000 and issues tokens for {@code http://localhost:9000}, because the
 * {@code iss} claim must equal the backend's issuer and clients reach the server on the host.
 */
public class DynamicFormStack implements AutoCloseable {

  private static final int AUTH_PORT = 9000;
  private static final String ISSUER = "http://localhost:" + AUTH_PORT;
  private static final String EXTRA_CONFIG_DIR = "/config/extra/";

  private final Network network = Network.newNetwork();
  private final OAuth2Container auth;
  private final PostgreSQLContainer database;
  private final DynamicFormContainer backend;

  public DynamicFormStack() {
    auth =
        new OAuth2Container()
            .withIssuerUrl(ISSUER)
            .withConfigFile("dynamicform/auth-defaults.yaml")
            .withNetwork(network)
            .withNetworkAliases("auth")
            .withCreateContainerCmdModifier(
                // modify, not replace: the HostConfig already carries the network mode
                cmd ->
                    cmd.getHostConfig()
                        .withPortBindings(
                            new PortBinding(
                                Ports.Binding.bindPort(AUTH_PORT), new ExposedPort(AUTH_PORT))));
    database =
        new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withNetwork(network)
            .withNetworkAliases("db");
    backend =
        new DynamicFormContainer()
            .withNetwork(network)
            .withDatabase(
                "jdbc:postgresql://db:5432/" + database.getDatabaseName(),
                database.getUsername(),
                database.getPassword())
            .withIssuerUri(ISSUER)
            .withJwkSetUri("http://auth:" + AUTH_PORT + "/oauth2/jwks");
  }

  /**
   * Adds auth server configuration (e.g. a {@code desktop-client}) on top of the default users
   * {@code admin/admin} and {@code user/user}. Maps such as clients merge by key; a {@code users}
   * list in the file replaces the default users.
   */
  public DynamicFormStack withAuthConfigFile(String classpathResource) {
    auth.withCopyFileToContainer(
            MountableFile.forClasspathResource(classpathResource),
            EXTRA_CONFIG_DIR + "application.yaml")
        .withEnv("SPRING_CONFIG_ADDITIONAL_LOCATION", "optional:file:" + EXTRA_CONFIG_DIR);
    return this;
  }

  public DynamicFormStack withLogConsumer(Consumer<OutputFrame> consumer) {
    auth.withLogConsumer(consumer);
    backend.withLogConsumer(consumer);
    database.withLogConsumer(consumer);
    return this;
  }

  public DynamicFormStack start() {
    auth.start();
    database.start();
    backend.start();
    return this;
  }

  public String backendUrl() {
    return backend.getBackendUrl();
  }

  public String authUrl() {
    return ISSUER;
  }

  @Override
  public void close() {
    backend.stop();
    database.stop();
    auth.stop();
    network.close();
  }
}
