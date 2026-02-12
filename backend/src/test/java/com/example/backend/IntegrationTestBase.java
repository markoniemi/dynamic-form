package com.example.backend;

import com.example.backend.config.SeleniumConfig;
import com.example.backend.config.TestcontainersConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import({TestcontainersConfig.class, SeleniumConfig.class})
@ActiveProfiles("test")
public abstract class IntegrationTestBase {}
