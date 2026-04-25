package com.example.backend.dto;

import lombok.Value;

@Value
public class ValidationErrorDto {
  String field;    // form field name, null for class-level errors
  String message;  // resolved human-readable string
  String code;     // short constraint name, e.g. "NotBlank"
}
