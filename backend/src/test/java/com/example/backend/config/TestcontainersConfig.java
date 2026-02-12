package com.example.backend.config;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
@Slf4j
public class TestcontainersConfig {
  @Bean(destroyMethod = "")
  @RestartScope
  public GenericContainer<?> authorizationServerContainer() {
    GenericContainer<?> authorizationServer =
        new GenericContainer<>(DockerImageName.parse("auth-server:latest"))
            .withExposedPorts(9000)
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withCreateContainerCmdModifier(getPortConfig())
            .waitingFor(Wait.forHttp("/.well-known/openid-configuration").forStatusCode(200));
    authorizationServer.start();
    return authorizationServer;
  }

  @Bean
  public CommandLineRunner ensureContainerStarted(GenericContainer<?> authorizationServerContainer) {
    return args -> log.info("Auth server container started: {}", authorizationServerContainer.isRunning());
  }

  private static @NonNull Consumer<CreateContainerCmd> getPortConfig() {
    return cmd ->
        cmd.withHostConfig(
            new HostConfig()
                .withPortBindings(
                    new PortBinding(Ports.Binding.bindPort(9000), new ExposedPort(9000))));
  }
}
