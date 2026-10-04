package com.example.backend.controller;

import static com.example.backend.util.SecurityUtils.getUsername;
import static com.example.backend.util.SecurityUtils.isAdmin;

import com.example.backend.dto.FormDataDto;
import com.example.backend.entity.FormData;
import com.example.backend.mapper.FormDataMapper;
import com.example.backend.service.FormDataService;
import java.util.List;
import java.util.Map;
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
    FormData formData = new FormData(key, data, getUsername(jwt));
    return formDataMapper.toDto(formDataService.create(key, formData));
  }

  @PutMapping("/submission/{id}")
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public FormDataDto updateSubmission(
      @PathVariable Long id,
      @RequestBody Map<String, Object> data,
      @AuthenticationPrincipal Jwt jwt) {
    String username = getUsername(jwt);
    return formDataMapper.toDto(formDataService.update(id, data, username));
  }

  @GetMapping
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public List<FormDataDto> getSubmissions(
      @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
    if (isAdmin(authentication)) {
      return formDataMapper.mapList(formDataService.getAll());
    } else {
      return formDataMapper.mapList(formDataService.getByOwner(getUsername(jwt)));
    }
  }

  @GetMapping("/submission/{id}")
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public FormDataDto getSubmissionById(
      @PathVariable Long id, @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
    String username = getUsername(jwt);
    FormData submission = formDataService.get(id);
    if (!submission.getSubmittedBy().equals(username) && !isAdmin(authentication)) {
      throw new SecurityException("You are not authorized to view this submission");
    }
    return formDataMapper.toDto(submission);
  }

  @DeleteMapping("/submission/{id}")
  @InterfaceLog
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public void deleteSubmission(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    formDataService.delete(id, getUsername(jwt));
  }
}
