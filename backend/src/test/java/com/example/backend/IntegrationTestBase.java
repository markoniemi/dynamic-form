package com.example.backend;

import com.example.backend.config.PlaywrightConfig;
import com.example.backend.config.TestcontainersConfig;
import org.springframework.boot.test.context.DynamicPropertyRegistry;
import org.springframework.boot.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import({TestcontainersConfig.class, PlaywrightConfig.class})
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

  @DynamicPropertySource
  static void oAuth2Properties(DynamicPropertyRegistry registry) {
    var container = TestcontainersConfig.getSharedContainer();
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
        container::getIssuerUrl);
    registry.add("spring.security.oauth2.client.provider.oauth2.issuer-uri",
        container::getIssuerUrl);
  }
}
