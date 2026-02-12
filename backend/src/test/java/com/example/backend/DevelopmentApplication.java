package com.example.backend;

import com.example.backend.config.TestcontainersConfig;
import org.springframework.boot.SpringApplication;

public class DevelopmentApplication {
  public static void main(String[] args) {
    SpringApplication.from(BackendApplication::main)
        .with(TestcontainersConfig.class)
        .withAdditionalProfiles("test")
        .run(args);
  }
}
