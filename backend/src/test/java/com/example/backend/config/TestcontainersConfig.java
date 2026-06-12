package com.example.backend.config;

import com.example.auth.testcontainers.Client;
import com.example.auth.testcontainers.OAuth2Container;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import java.util.Set;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@TestConfiguration(proxyBeanMethods = false)
@Slf4j
public class TestcontainersConfig {

  @Bean(destroyMethod = "")
  public OAuth2Container authorizationServerContainer() {
    OAuth2Container authorizationServer =
        new OAuth2Container()
            .withUser("admin", "admin", "USER", "ADMIN")
            .withOAuth2Client(new Client(
                "frontend-client",
                "",
                Set.of("authorization_code"),
                Set.of("http://localhost:8080", "http://localhost:5173"),
                Set.of("openid", "profile", "email")))
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withCreateContainerCmdModifier(getPortConfig());
    authorizationServer.start();
    return authorizationServer;
    }

  private static @NonNull Consumer<CreateContainerCmd> getPortConfig() {
    return cmd ->
        cmd.withHostConfig(
            new HostConfig()
                .withPortBindings(
                    new PortBinding(Ports.Binding.bindPort(9000), new ExposedPort(9000))));
  }
}
