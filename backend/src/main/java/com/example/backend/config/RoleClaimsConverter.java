package com.example.backend.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Maps token claims to Spring authorities. Keeps the default {@code SCOPE_*} authorities and adds
 * {@code ROLE_*} authorities from the {@code roles} claim (oauth2-server) and the {@code
 * cognito:groups} claim (AWS Cognito). Values without the {@code ROLE_} prefix get it added, so
 * both {@code ADMIN} and {@code ROLE_ADMIN} become {@code ROLE_ADMIN}.
 */
public class RoleClaimsConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  private static final List<String> ROLE_CLAIMS = List.of("roles", "cognito:groups");
  private static final String ROLE_PREFIX = "ROLE_";

  private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopes.convert(jwt));
    for (String claim : ROLE_CLAIMS) {
      List<String> roles = jwt.getClaimAsStringList(claim);
      if (roles == null) {
        continue;
      }
      for (String role : roles) {
        authorities.add(
            new SimpleGrantedAuthority(
                role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role));
      }
    }
    return authorities;
  }
}
