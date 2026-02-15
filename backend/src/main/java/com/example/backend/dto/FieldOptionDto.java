package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FieldOptionDto {

  @NotBlank(message = "Option value is required")
  String value;

  @NotBlank(message = "Option label is required")
  String label;

}

