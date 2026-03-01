package com.example.backend.service;

import com.example.backend.entity.FormData;
import com.example.backend.repository.FormDataRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.log.InterfaceLog;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@InterfaceLog
public class FormDataService {

  private final FormDataRepository formDataRepository;
  private final FormService formService;

  @InterfaceLog
  public FormData createFormSubmission(@NotNull String formKey, @Valid FormData formData) {
    // Validate that the form exists
    formService.getForm(formKey);

    // Set the form key
    formData.setFormKey(formKey);

    // Save the submission
    return formDataRepository.save(formData);
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
  public List<FormData> getFormSubmissionsByKey(@NotNull String formKey) {
    return formDataRepository.findByFormKeyOrderBySubmittedAtDesc(formKey);
  }

  @InterfaceLog
  public void deleteFormSubmission(@NotNull Long id) {
    formDataRepository.deleteById(id);
  }
}
