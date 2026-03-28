package com.example.backend.util;

import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class SecurityUtils {

  public static String getUsername(Jwt jwt) {
    if (jwt == null) {
      return "anonymous";
    }
    String username = jwt.getClaimAsString("preferred_username");
    if (username == null) {
      username = jwt.getSubject();
    }
    return username;
  }

  public static boolean isAdmin(Authentication authentication) {
    if (authentication == null) {
      return false;
    }
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    if (authorities == null) {
      return false;
    }
    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(a -> a.equals("ROLE_ADMIN"));
  }
}
