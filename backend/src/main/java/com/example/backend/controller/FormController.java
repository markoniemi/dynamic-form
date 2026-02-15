package com.example.backend.controller;

import com.example.backend.dto.FormDto;
import com.example.backend.entity.Form;
import com.example.backend.mapper.FormMapper;
import com.example.backend.service.FormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.log.InterfaceLog;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
  public Set<String> getAvailableForms() {
    return formService.getAvailableFormKeys();
  }

  @GetMapping("/all")
  @InterfaceLog
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
  public void deleteForm(@PathVariable String key) {
    formService.deleteForm(key);
  }

}
