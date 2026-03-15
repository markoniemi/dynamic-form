package com.example.backend.controller;

import static com.example.backend.util.SecurityUtils.getUsername;
import static com.example.backend.util.SecurityUtils.isAdmin;

import com.example.backend.dto.FormDataDto;
import com.example.backend.entity.FormData;
import com.example.backend.mapper.FormDataMapper;
import com.example.backend.service.FormDataService;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.log.InterfaceLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    String username = getUsername(jwt);
    FormData formData = new FormData(key, data, username);
    FormData savedFormData = formDataService.createFormSubmission(key, formData);
    return formDataMapper.toDto(savedFormData);
  }

  @PutMapping("/submission/{id}")
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public FormDataDto updateSubmission(
      @PathVariable Long id,
      @RequestBody Map<String, Object> data,
      @AuthenticationPrincipal Jwt jwt) {
    String username = getUsername(jwt);
    FormData updatedFormData = formDataService.updateFormSubmission(id, data, username);
    return formDataMapper.toDto(updatedFormData);
  }

  @GetMapping
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public List<FormDataDto> getAllSubmissions(
      @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
    String username = getUsername(jwt);
    if (isAdmin(authentication)) {
      return formDataService.getAllFormSubmissions().stream().map(formDataMapper::toDto).toList();
    } else {
      return formDataService.getFormSubmissionsByOwner(username).stream().map(formDataMapper::toDto).toList();
    }
  }

  @GetMapping("/{key}")
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public List<FormDataDto> getSubmissionsByFormKey(
      @PathVariable String key,
      @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
    String username = getUsername(jwt);
    if (isAdmin(authentication)) {
      return formDataService.getFormSubmissionsByKey(key).stream().map(formDataMapper::toDto).toList();
    } else {
      return formDataService.getFormSubmissionsByKeyAndOwner(key, username).stream()
          .map(formDataMapper::toDto)
          .toList();
    }
  }

  @GetMapping("/submission/{id}")
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public FormDataDto getSubmissionById(
      @PathVariable Long id,
      @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
    String username = getUsername(jwt);
    return formDataService
        .getFormSubmissionById(id)
        .map(submission -> {
          boolean isAdmin = isAdmin(authentication);

          if (!submission.getSubmittedBy().equals(username) && !isAdmin) {
            throw new SecurityException("You are not authorized to view this submission");
          }
          return formDataMapper.toDto(submission);
        })
        .orElseThrow(() -> new NoSuchElementException("Form submission not found: " + id));
  }

  @DeleteMapping("/submission/{id}")
  @InterfaceLog
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public void deleteSubmission(@PathVariable Long id) {
    formDataService.deleteFormSubmission(id);
  }
}
