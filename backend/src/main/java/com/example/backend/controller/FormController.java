package com.example.backend.controller;

import com.example.backend.dto.FormDto;
import com.example.backend.dto.FormListItemDto;
import com.example.backend.mapper.FormMapper;
import com.example.backend.service.FormService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.log.InterfaceLog;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
@Slf4j
@InterfaceLog
public class FormController {

  private final FormService formService;
  private final FormMapper formMapper;

  @GetMapping
  @InterfaceLog
  public List<FormListItemDto> getAvailableForms() {
    return formService.getAvailableForms();
  }

  @GetMapping("/all")
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public List<FormDto> getAllForms() {
    return formService.getAllForms().stream().map(formMapper::toDto).toList();
  }

  @GetMapping("/{key}")
  @InterfaceLog
  @PreAuthorize("isAuthenticated()")
  public FormDto getForm(@PathVariable String key) {
    return formMapper.toDto(formService.getForm(key));
  }

  @PostMapping
  @InterfaceLog
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public FormDto createForm(@Valid @RequestBody FormDto dto) {
    if (formService.existsByFormKey(dto.getFormKey())) {
      throw new IllegalArgumentException("Form with key '" + dto.getFormKey() + "' already exists");
    }
    return formMapper.toDto(formService.saveForm(formMapper.toEntity(dto)));
  }

  @PutMapping("/{key}")
  @InterfaceLog
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public FormDto updateForm(@PathVariable String key, @Valid @RequestBody FormDto dto) {
    return formMapper.toDto(formService.updateForm(key, formMapper.toEntity(dto)));
  }

  @DeleteMapping("/{key}")
  @InterfaceLog
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public void deleteForm(@PathVariable String key) {
    formService.deleteForm(key);
  }
}
