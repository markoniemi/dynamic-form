package com.example.backend.config;

import com.github.markoniemi.oauth2server.testcontainers.Client;
import com.github.markoniemi.oauth2server.testcontainers.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
@Slf4j
public class TestcontainersConfig {
  private static Container sharedContainer;

  @Bean(destroyMethod = "stop")
  public Container oAuth2ServerContainer() {
    if (sharedContainer == null) {
      sharedContainer = new Container()
          .withUser("testuser", "password", "USER")
          .withUser("admin", "password", "ADMIN", "USER")
          .withOAuth2Client(new Client("test-client", "test-secret")
              .withRedirectUri("http://localhost:8080/callback")
              .withRedirectUri("http://localhost:3000/callback")
              .withScopes("openid", "profile", "email"));
      sharedContainer.start();
    }
    return sharedContainer;
  }

  public static Container getSharedContainer() {
    return sharedContainer;
  }
}
