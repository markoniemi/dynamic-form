package com.example.backend.service;

import com.example.backend.entity.FormData;
import com.example.backend.repository.FormDataRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.log.InterfaceLog;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@InterfaceLog
public class FormDataService {

  private final FormDataRepository formDataRepository;
  private final FormService formService;

  @InterfaceLog
  @Transactional
  public FormData createFormSubmission(@NotNull String formKey, @Valid FormData formData) {
    // Validate that the form exists
    formService.getForm(formKey);

    // Set the form key
    formData.setFormKey(formKey);

    // Save the submission
    return formDataRepository.save(formData);
  }

  @InterfaceLog
  @Transactional
  public FormData updateFormSubmission(@NotNull Long id, @NotNull Map<String, Object> data, String username) {
    FormData existing = formDataRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + id));

    if (!existing.getSubmittedBy().equals(username)) {
      throw new AccessDeniedException("You are not authorized to update this submission");
    }

    existing.setData(data);
    log.info("Updating submission: {}", id);
    return formDataRepository.save(existing);
  }

  @InterfaceLog
  public Optional<FormData> getFormSubmissionById(@NotNull Long id) {
    return formDataRepository.findById(id);
  }

  @InterfaceLog
  public List<FormData> getAllFormSubmissions() {
    return formDataRepository.findAll();
  }

  @InterfaceLog
  public List<FormData> getFormSubmissionsByOwner(@NotNull String username) {
    return formDataRepository.findBySubmittedByOrderBySubmittedAtDesc(username);
  }

  @InterfaceLog
  public List<FormData> getFormSubmissionsByKey(@NotNull String formKey) {
    return formDataRepository.findByFormKeyOrderBySubmittedAtDesc(formKey);
  }

  @InterfaceLog
  public List<FormData> getFormSubmissionsByKeyAndOwner(@NotNull String formKey, @NotNull String username) {
    return formDataRepository.findByFormKeyAndSubmittedByOrderBySubmittedAtDesc(formKey, username);
  }

  @InterfaceLog
  @Transactional
  public void deleteFormSubmission(@NotNull Long id, @NotNull String username) {
    FormData existing = formDataRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + id));

    if (!existing.getSubmittedBy().equals(username)) {
      throw new AccessDeniedException("You are not authorized to delete this submission");
    }

    formDataRepository.deleteById(id);
  }
}
