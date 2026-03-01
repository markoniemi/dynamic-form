package com.example.backend.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Value
public class ErrorDto {
  String message;
  Map<String, String> errors;

  public static ErrorDto of(Exception e) {
    return new ErrorDto(
        e.getClass().getSimpleName(), Map.of(e.getClass().getSimpleName(), e.getMessage()));
  }

  public static ErrorDto of(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new java.util.HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
    ex.getBindingResult()
        .getGlobalErrors()
        .forEach(error -> errors.put(error.getObjectName(), error.getDefaultMessage()));
    return new ErrorDto("Validation failed", errors);
  }

  public static ErrorDto of(ConstraintViolationException ex) {
    return new ErrorDto("Validation failed", getErrors(ex.getConstraintViolations()));
  }

  private static Map<String, String> getErrors(Set<ConstraintViolation<?>> violations) {
    return violations.stream()
        .collect(
            Collectors.toMap(
                error -> error.getPropertyPath().toString(),
                ConstraintViolation::getMessageTemplate));
  }
}
