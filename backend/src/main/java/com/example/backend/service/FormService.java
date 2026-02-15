package com.example.backend.service;

import com.example.backend.entity.Form;
import com.example.backend.repository.FormRepository;
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

  private final FormRepository formRepository;

  @InterfaceLog
  public Set<String> getAvailableFormKeys() {
    return formRepository.findAll().stream()
        .map(Form::getFormKey)
        .collect(Collectors.toSet());
  }

  @InterfaceLog
  public Form getForm(String formKey) {
    return formRepository.findByFormKey(formKey)
        .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formKey));
  }

  @InterfaceLog
  public Optional<Form> findByFormKey(String formKey) {
    return formRepository.findByFormKey(formKey);
  }

  @InterfaceLog
  public List<Form> getAllForms() {
    return formRepository.findAll();
  }

  @InterfaceLog
  @Transactional
  public Form saveForm(Form form) {
    log.info("Saving form definition: {}", form.getFormKey());
    return formRepository.save(form);
  }

  @InterfaceLog
  @Transactional
  public Form updateForm(String formKey, Form updatedDefinition) {
    Form existing = formRepository.findByFormKey(formKey)
        .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formKey));

    existing.setTitle(updatedDefinition.getTitle());
    existing.setDescription(updatedDefinition.getDescription());
    existing.setFields(updatedDefinition.getFields());

    log.info("Updating form definition: {}", formKey);
    return formRepository.save(existing);
  }

  @InterfaceLog
  @Transactional
  public void deleteForm(String formKey) {
    Form existing = formRepository.findByFormKey(formKey)
        .orElseThrow(() -> new IllegalArgumentException("Form not found: " + formKey));
    formRepository.delete(existing);
    log.info("Deleted form definition: {}", formKey);
  }

  @InterfaceLog
  public boolean existsByFormKey(String formKey) {
    return formRepository.existsByFormKey(formKey);
  }

}
