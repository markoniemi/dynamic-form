package com.example.backend.service;

import com.example.backend.entity.FormDefinition;
import com.example.backend.repository.FormDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.log.InterfaceLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@InterfaceLog
public class FormService {

  private final FormDefinitionRepository formDefinitionRepository;

  @InterfaceLog
  public Set<String> getAvailableFormKeys() {
    return formDefinitionRepository.findAll().stream()
        .map(FormDefinition::getFormKey)
        .collect(Collectors.toSet());
  }

  @InterfaceLog
  public FormDefinition getFormDefinition(String formKey) {
    return formDefinitionRepository.findByFormKey(formKey)
        .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formKey));
  }

  @InterfaceLog
  public Optional<FormDefinition> findByFormKey(String formKey) {
    return formDefinitionRepository.findByFormKey(formKey);
  }

  @InterfaceLog
  public List<FormDefinition> getAllFormDefinitions() {
    return formDefinitionRepository.findAll();
  }

  @InterfaceLog
  @Transactional
  public FormDefinition saveFormDefinition(FormDefinition formDefinition) {
    log.info("Saving form definition: {}", formDefinition.getFormKey());
    return formDefinitionRepository.save(formDefinition);
  }

  @InterfaceLog
  @Transactional
  public FormDefinition updateFormDefinition(String formKey, FormDefinition updatedDefinition) {
    FormDefinition existing = formDefinitionRepository.findByFormKey(formKey)
        .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formKey));

    existing.setTitle(updatedDefinition.getTitle());
    existing.setDescription(updatedDefinition.getDescription());
    existing.setFields(updatedDefinition.getFields());

    log.info("Updating form definition: {}", formKey);
    return formDefinitionRepository.save(existing);
  }

  @InterfaceLog
  @Transactional
  public void deleteFormDefinition(String formKey) {
    FormDefinition existing = formDefinitionRepository.findByFormKey(formKey)
        .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formKey));
    formDefinitionRepository.delete(existing);
    log.info("Deleted form definition: {}", formKey);
  }

  @InterfaceLog
  public boolean existsByFormKey(String formKey) {
    return formDefinitionRepository.existsByFormKey(formKey);
  }

}
