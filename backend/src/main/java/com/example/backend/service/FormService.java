package com.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.log.InterfaceLog;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@InterfaceLog
public class FormService {

    private final ObjectMapper objectMapper;
    private final Map<String, JsonNode> formDefinitions = new ConcurrentHashMap<>();

    @PostConstruct
    @InterfaceLog
    public void loadFormDefinitions() {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:/forms/*.json");

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null) {
                    String formKey = filename.replace(".json", "");
                    JsonNode formDefinition = objectMapper.readTree(resource.getInputStream());
                    formDefinitions.put(formKey, formDefinition);
                    log.info("Loaded form definition: {}", formKey);
                }
            }

            log.info("Total forms loaded: {}", formDefinitions.size());
        } catch (IOException e) {
            log.error("Failed to load form definitions", e);
            throw new RuntimeException("Failed to load form definitions", e);
        }
    }

    @InterfaceLog
    public Set<String> getAvailableFormKeys() {
        return formDefinitions.keySet();
    }

    @InterfaceLog
    public JsonNode getFormDefinition(String formKey) {
        JsonNode definition = formDefinitions.get(formKey);
        if (definition == null) {
            throw new IllegalArgumentException("Form not found: " + formKey);
        }
        return definition;
    }

}
