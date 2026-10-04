package com.example.backend.testdata;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Test users with the roles the auth server's demo users have. Use the username constants for
 * {@code submittedBy} and service calls, and the request post-processors to authenticate MockMvc
 * requests, e.g. {@code .with(TestUsers.user())}.
 */
public final class TestUsers {

  /** Regular user. */
  public static final String USER = "user";

  /** Administrator; also serves as "someone else" when a test needs another owner than USER. */
  public static final String ADMIN = "admin";

  private TestUsers() {}

  public static RequestPostProcessor user() {
    return authenticatedAs(USER, "ROLE_USER");
  }

  public static RequestPostProcessor admin() {
    return authenticatedAs(ADMIN, "ROLE_USER", "ROLE_ADMIN");
  }

  private static RequestPostProcessor authenticatedAs(String username, String... roles) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(username)
            .claim("preferred_username", username)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    List<GrantedAuthority> authorities =
        Arrays.stream(roles).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
    return SecurityMockMvcRequestPostProcessors.authentication(
        new JwtAuthenticationToken(jwt, authorities));
  }
}
