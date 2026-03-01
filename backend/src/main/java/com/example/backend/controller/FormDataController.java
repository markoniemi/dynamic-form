package com.example.backend.controller;

import com.example.backend.dto.FormDataDto;
import com.example.backend.entity.FormData;
import com.example.backend.mapper.FormDataMapper;
import com.example.backend.service.FormDataService;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.log.InterfaceLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/form-data")
@RequiredArgsConstructor
@Slf4j
@InterfaceLog
public class FormDataController {

  private final FormDataService formDataService;
  private final FormDataMapper formDataMapper;

  @PostMapping("/{key}")
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public FormDataDto submitForm(
      @PathVariable String key,
      @RequestBody Map<String, Object> data,
      @AuthenticationPrincipal Jwt jwt) {
    String username = jwt.getClaimAsString("sub");
    FormData formData = new FormData(key, data, username);
    FormData savedFormData = formDataService.createFormSubmission(key, formData);
    return formDataMapper.toDto(savedFormData);
  }

  @GetMapping
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public List<FormDataDto> getAllSubmissions() {
    return formDataService.getAllFormSubmissions().stream()
        .map(formDataMapper::toDto)
        .collect(Collectors.toList());
  }

  @GetMapping("/{key}")
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public List<FormDataDto> getSubmissionsByFormKey(@PathVariable String key) {
    return formDataService.getFormSubmissionsByKey(key).stream()
        .map(formDataMapper::toDto)
        .collect(Collectors.toList());
  }

  @GetMapping("/submission/{id}")
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public FormDataDto getSubmissionById(@PathVariable Long id) {
    return formDataService
        .getFormSubmissionById(id)
        .map(formDataMapper::toDto)
        .orElseThrow(() -> new NoSuchElementException("Form submission not found: " + id));
  }

  @DeleteMapping("/submission/{id}")
  @InterfaceLog
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public void deleteSubmission(@PathVariable Long id) {
    formDataService.deleteFormSubmission(id);
  }
}
