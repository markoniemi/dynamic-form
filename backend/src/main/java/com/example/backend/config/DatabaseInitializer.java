package com.example.backend.config;

import com.example.backend.entity.FormDefinition;
import com.example.backend.entity.FormFieldDefinition;
import com.example.backend.entity.FormFieldOption;
import com.example.backend.repository.FormDefinitionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

  private final FormDefinitionRepository formDefinitionRepository;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public void run(String... args) {
    loadFormDefinitionsFromResources();
  }

  private void loadFormDefinitionsFromResources() {
    try {
      ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources("classpath:/forms/*.json");

      int loadedCount = 0;
      for (Resource resource : resources) {
        String filename = resource.getFilename();
        if (filename != null) {
          String formKey = filename.replace(".json", "");

          if (formDefinitionRepository.existsByFormKey(formKey)) {
            log.debug("Form definition '{}' already exists, skipping", formKey);
            continue;
          }

          FormDefinition formDefinition = parseFormDefinition(formKey, resource);
          formDefinitionRepository.save(formDefinition);
          loadedCount++;
          log.info("Loaded form definition: {}", formKey);
        }
      }

      log.info("Database initialization complete. Loaded {} new form definition(s). Total forms: {}",
          loadedCount, formDefinitionRepository.count());
    } catch (IOException e) {
      log.error("Failed to load form definitions from resources", e);
      throw new RuntimeException("Failed to load form definitions", e);
    }
  }

  private FormDefinition parseFormDefinition(String formKey, Resource resource) throws IOException {
    JsonNode rootNode = objectMapper.readTree(resource.getInputStream());

    String title = rootNode.path("title").asText("");
    String description = rootNode.path("description").asText("");

    List<FormFieldDefinition> fields = new ArrayList<>();
    JsonNode fieldsNode = rootNode.path("fields");
    if (fieldsNode.isArray()) {
      for (JsonNode fieldNode : fieldsNode) {
        FormFieldDefinition field = parseFieldDefinition(fieldNode);
        fields.add(field);
      }
    }

    return FormDefinition.builder()
        .formKey(formKey)
        .title(title)
        .description(description)
        .fields(fields)
        .build();
  }

  private FormFieldDefinition parseFieldDefinition(JsonNode fieldNode) {
    String name = fieldNode.path("name").asText("");
    String label = fieldNode.path("label").asText("");
    String type = fieldNode.path("type").asText("text");
    boolean required = fieldNode.path("required").asBoolean(false);
    String placeholder = fieldNode.path("placeholder").asText(null);

    List<FormFieldOption> options = new ArrayList<>();
    JsonNode optionsNode = fieldNode.path("options");
    if (optionsNode.isArray()) {
      for (JsonNode optionNode : optionsNode) {
        FormFieldOption option = FormFieldOption.builder()
            .value(optionNode.path("value").asText(""))
            .label(optionNode.path("label").asText(""))
            .build();
        options.add(option);
      }
    }

    return FormFieldDefinition.builder()
        .name(name)
        .label(label)
        .type(type)
        .required(required)
        .placeholder(placeholder)
        .options(options.isEmpty() ? null : options)
        .build();
  }
}

