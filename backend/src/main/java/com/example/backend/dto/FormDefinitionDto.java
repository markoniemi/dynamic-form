package com.example.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class FormDefinitionDto {

  Long id;

  @NotBlank(message = "Form key is required")
  @Pattern(regexp = "^[a-z0-9-]+$", message = "Form key must contain only lowercase letters, numbers, and hyphens")
  String formKey;

  @NotBlank(message = "Title is required")
  String title;

  String description;

  @NotEmpty(message = "At least one field is required")
  @Valid
  List<FormFieldDto> fields;

}

