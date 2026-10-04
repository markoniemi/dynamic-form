package com.example.backend.config;

import com.example.backend.entity.Form;
import com.example.backend.repository.FormRepository;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

  private final FormRepository formRepository;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public void run(String... args) {
    loadFormsFromResources();
  }

  private void loadFormsFromResources() {
    try {
      ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources("classpath:/forms/*.json");

      int loadedCount = 0;
      for (Resource resource : resources) {
        Form form = objectMapper.readValue(resource.getInputStream(), Form.class);

        if (formRepository.existsByFormKey(form.getFormKey())) {
          log.debug("Form definition '{}' already exists, skipping", form.getFormKey());
          continue;
        }

        formRepository.save(form);
        loadedCount++;
        log.info("Loaded form definition: {}", form.getFormKey());
      }

      log.info(
          "Database initialization complete. Loaded {} new form definition(s). Total forms: {}",
          loadedCount,
          formRepository.count());
    } catch (IOException e) {
      log.error("Failed to load form definitions from resources", e);
      throw new RuntimeException("Failed to load form definitions", e);
    }
  }
}
