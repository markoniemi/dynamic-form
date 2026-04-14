package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String oauth2IssuerUri;

  @GetMapping("/oauth2-issuer-uri")
  public String getOauth2IssuerUri() {
    return oauth2IssuerUri;
  }
}
