package com.example.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class RoleClaimsConverterTest {

  private final RoleClaimsConverter converter = new RoleClaimsConverter();

  @Test
  void mapsRolesClaimAndAddsRolePrefix() {
    Jwt jwt = jwt("roles", List.of("USER", "ADMIN"));

    assertThat(authorities(jwt)).contains("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  void keepsExistingRolePrefix() {
    Jwt jwt = jwt("roles", List.of("ROLE_ADMIN"));

    assertThat(authorities(jwt)).containsExactly("ROLE_ADMIN");
  }

  @Test
  void mapsCognitoGroups() {
    Jwt jwt = jwt("cognito:groups", List.of("ROLE_ADMIN"));

    assertThat(authorities(jwt)).containsExactly("ROLE_ADMIN");
  }

  @Test
  void keepsScopeAuthorities() {
    Jwt jwt = jwt("scope", "openid profile");

    assertThat(authorities(jwt)).containsExactly("SCOPE_openid", "SCOPE_profile");
  }

  @Test
  void tokenWithoutRoleClaimsHasNoRoles() {
    Jwt jwt = jwt("sub2", "x");

    assertThat(authorities(jwt)).noneMatch(a -> a.startsWith("ROLE_"));
  }

  private List<String> authorities(Jwt jwt) {
    return converter.convert(jwt).stream().map(GrantedAuthority::getAuthority).toList();
  }

  private static Jwt jwt(String claim, Object value) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("user")
        .claim(claim, value)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}
