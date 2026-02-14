package com.example.backend.controller;

import com.example.backend.dto.FormDefinitionDto;
import com.example.backend.entity.FormDefinition;
import com.example.backend.mapper.FormDefinitionMapper;
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
  private final FormDefinitionMapper formDefinitionMapper;

  @GetMapping
  @InterfaceLog
  public Set<String> getAvailableForms() {
    return formService.getAvailableFormKeys();
  }

  @GetMapping("/all")
  @InterfaceLog
  public List<FormDefinitionDto> getAllFormDefinitions() {
    return formService.getAllFormDefinitions().stream()
        .map(formDefinitionMapper::toDto)
        .collect(Collectors.toList());
  }

  @GetMapping("/{key}")
  @InterfaceLog
  public FormDefinitionDto getFormDefinition(@PathVariable String key) {
    FormDefinition definition = formService.getFormDefinition(key);
    return formDefinitionMapper.toDto(definition);
  }

  @PostMapping
  @InterfaceLog
  @ResponseStatus(HttpStatus.CREATED)
  public FormDefinitionDto createFormDefinition(@Valid @RequestBody FormDefinitionDto dto) {
    if (formService.existsByFormKey(dto.getFormKey())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Form with key '" + dto.getFormKey() + "' already exists");
    }
    FormDefinition entity = formDefinitionMapper.toEntity(dto);
    FormDefinition saved = formService.saveFormDefinition(entity);
    return formDefinitionMapper.toDto(saved);
  }

  @PutMapping("/{key}")
  @InterfaceLog
  public FormDefinitionDto updateFormDefinition(
      @PathVariable String key,
      @Valid @RequestBody FormDefinitionDto dto) {
    FormDefinition entity = formDefinitionMapper.toEntity(dto);
    FormDefinition updated = formService.updateFormDefinition(key, entity);
    return formDefinitionMapper.toDto(updated);
  }

  @DeleteMapping("/{key}")
  @InterfaceLog
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteFormDefinition(@PathVariable String key) {
    formService.deleteFormDefinition(key);
  }

}
