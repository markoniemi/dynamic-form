package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

  private final String oauth2IssuerUri;

  public ConfigController(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String oauth2IssuerUri) {
    this.oauth2IssuerUri = oauth2IssuerUri;
  }

  @GetMapping("/oauth2-issuer-uri")
  public String getOauth2IssuerUri() {
    return oauth2IssuerUri;
  }
}
