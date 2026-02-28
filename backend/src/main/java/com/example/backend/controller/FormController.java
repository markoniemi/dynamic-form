package com.example.backend.controller;

import com.example.backend.dto.FormDto;
import com.example.backend.dto.FormListItemDto;
import com.example.backend.entity.Form;
import com.example.backend.mapper.FormMapper;
import com.example.backend.service.FormService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.log.InterfaceLog;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public List<FormDto> getAllForms() {
    return formService.getAllForms().stream()
        .map(formMapper::toDto)
        .collect(Collectors.toList());
  }

  @GetMapping("/{key}")
  @InterfaceLog
  public FormDto getForm(@PathVariable String key) {
    Form definition = formService.getForm(key);
    return formMapper.toDto(definition);
  }

  @PostMapping
  @InterfaceLog
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public FormDto createForm(@Valid @RequestBody FormDto dto) {
    if (formService.existsByFormKey(dto.getFormKey())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Form with key '" + dto.getFormKey() + "' already exists");
    }
    Form entity = formMapper.toEntity(dto);
    Form saved = formService.saveForm(entity);
    return formMapper.toDto(saved);
  }

  @PutMapping("/{key}")
  @InterfaceLog
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public FormDto updateForm(
      @PathVariable String key,
      @Valid @RequestBody FormDto dto) {
    Form entity = formMapper.toEntity(dto);
    Form updated = formService.updateForm(key, entity);
    return formMapper.toDto(updated);
  }

  @DeleteMapping("/{key}")
  @InterfaceLog
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public void deleteForm(@PathVariable String key) {
    formService.deleteForm(key);
  }

}
