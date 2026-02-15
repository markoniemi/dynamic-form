package com.example.backend.controller;

import com.example.backend.dto.ErrorDto;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorDto> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.warn("IllegalArgumentException: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorDto.of(ex));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorDto> handleValidationException(MethodArgumentNotValidException ex) {
    log.warn("Validation failed: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorDto.of(ex));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorDto> handleConstraintViolationException(
      ConstraintViolationException exception) {
    log.warn("ConstraintViolationException: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorDto.of(exception));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorDto> handleNoResourceFoundException(NoResourceFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorDto.of(ex));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDto> handleGenericException(Exception ex) {
    log.error("Unhandled exception: ", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorDto.of(ex));
  }
}
