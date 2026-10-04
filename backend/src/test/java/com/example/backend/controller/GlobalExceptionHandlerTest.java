package com.example.backend.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.example.backend.dto.FieldDto;
import com.example.backend.dto.FormDto;
import com.example.backend.dto.ValidationErrorDto;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  private static void assertProblem(
      ResponseEntity<ProblemDetail> response, HttpStatus status, String detail) {
    assertEquals(status, response.getStatusCode());
    assertEquals(status.value(), response.getBody().getStatus());
    assertEquals(detail, response.getBody().getDetail());
  }

  @Test
  void illegalArgumentReturnsBadRequest() {
    assertProblem(
        handler.handleIllegalArgumentException(new IllegalArgumentException("bad input")),
        HttpStatus.BAD_REQUEST,
        "bad input");
  }

  @Test
  void noSuchElementReturnsNotFound() {
    assertProblem(
        handler.handleNoSuchElementException(new NoSuchElementException("missing")),
        HttpStatus.NOT_FOUND,
        "missing");
  }

  @Test
  void securityExceptionReturnsForbidden() {
    assertProblem(
        handler.handleSecurityException(new SecurityException("not yours")),
        HttpStatus.FORBIDDEN,
        "not yours");
  }

  @Test
  void accessDeniedReturnsForbidden() {
    assertProblem(
        handler.handleAccessDeniedException(new AccessDeniedException("Access Denied")),
        HttpStatus.FORBIDDEN,
        "Access Denied");
  }

  @Test
  void illegalStateReturnsConflict() {
    assertProblem(
        handler.handleIllegalStateException(new IllegalStateException("conflict")),
        HttpStatus.CONFLICT,
        "conflict");
  }

  @Test
  void unexpectedExceptionReturnsInternalServerErrorWithoutDetails() {
    assertProblem(
        handler.handleGenericException(new RuntimeException("secret internals")),
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error");
  }

  @Test
  void constraintViolationReturnsLeafFieldNameAndConstraintName() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    FormDto dto =
        FormDto.builder()
            .formKey("form1")
            .title("Title")
            .fields(List.of(FieldDto.builder().label("Label").type("text").build()))
            .build();

    ResponseEntity<ProblemDetail> response =
        handler.handleConstraintViolationException(
            new ConstraintViolationException(validator.validate(dto)));

    assertProblem(response, HttpStatus.BAD_REQUEST, "Validation failed");
    assertEquals(
        List.of(new ValidationErrorDto("name", "Field name is required", "NotBlank")),
        response.getBody().getProperties().get("errors"));
  }
}
