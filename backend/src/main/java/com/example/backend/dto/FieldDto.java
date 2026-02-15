package com.example.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class FieldDto {

  @NotBlank(message = "Field name is required")
  @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9]*$", message = "Field name must start with a letter and contain only alphanumeric characters")
  String name;

  @NotBlank(message = "Field label is required")
  String label;

  @NotBlank(message = "Field type is required")
  @Pattern(regexp = "^(text|email|tel|number|date|textarea|select|radio|checkbox)$", message = "Invalid field type")
  String type;

  boolean required;

  String placeholder;

  @Valid
  List<FieldOptionDto> options;

}

