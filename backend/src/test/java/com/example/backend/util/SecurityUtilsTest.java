package com.example.backend.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityUtilsTest {

  private static Jwt.Builder jwt() {
    return Jwt.withTokenValue("token").header("alg", "none");
  }

  @Test
  void getUsernamePrefersPreferredUsernameClaim() {
    Jwt token = jwt().claim("preferred_username", "alice").subject("subject-1").build();

    assertEquals("alice", SecurityUtils.getUsername(token));
  }

  @Test
  void getUsernameFallsBackToSubject() {
    Jwt token = jwt().subject("subject-1").build();

    assertEquals("subject-1", SecurityUtils.getUsername(token));
  }

  @Test
  void getUsernameReturnsAnonymousForMissingJwt() {
    assertEquals("anonymous", SecurityUtils.getUsername(null));
  }

  @Test
  void isAdminIsTrueForRoleAdmin() {
    assertTrue(SecurityUtils.isAdmin(new TestingAuthenticationToken("user", null, "ROLE_ADMIN")));
  }

  @Test
  void isAdminIsFalseForOtherRoles() {
    assertFalse(SecurityUtils.isAdmin(new TestingAuthenticationToken("user", null, "ROLE_USER")));
  }

  @Test
  void isAdminIsFalseForMissingAuthentication() {
    assertFalse(SecurityUtils.isAdmin(null));
  }

  @Test
  void isAdminIsFalseWhenAuthoritiesAreNull() {
    Authentication authentication = mock(Authentication.class);
    doReturn(null).when(authentication).getAuthorities();

    assertFalse(SecurityUtils.isAdmin(authentication));
  }
}
