package com.example.backend.controller;

import com.example.backend.dto.ValidationErrorDto;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.NoSuchElementException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.warn("IllegalArgumentException: {}", ex.getMessage());
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex) {
    log.warn("Validation failed: {}", ex.getMessage());
    List<ValidationErrorDto> errors = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> new ValidationErrorDto(
            fe.getField(),
            fe.getDefaultMessage(),
            firstCode(fe.getCodes())))
        .toList();
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    pd.setProperty("errors", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolationException(
      ConstraintViolationException exception) {
    log.warn("ConstraintViolationException: {}", exception.getMessage());
    List<ValidationErrorDto> errors = exception.getConstraintViolations().stream()
        .map(cv -> new ValidationErrorDto(
            leafPath(cv.getPropertyPath()),
            cv.getMessage(),
            cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()))
        .toList();
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    pd.setProperty("errors", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ProblemDetail> handleNoResourceFoundException(NoResourceFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ProblemDetail> handleNoSuchElementException(NoSuchElementException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ProblemDetail> handleSecurityException(SecurityException ex) {
    log.warn("SecurityException: {}", ex.getMessage());
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ProblemDetail> handleIllegalStateException(IllegalStateException ex) {
    log.warn("IllegalStateException: {}", ex.getMessage());
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
    log.error("Unhandled exception: ", ex);
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
  }

  private String firstCode(String[] codes) {
    if (codes == null || codes.length == 0) return "ValidationError";
    String code = codes[0];
    int lastDot = code.lastIndexOf('.');
    return lastDot >= 0 ? code.substring(0, lastDot) : code;
  }

  private String leafPath(jakarta.validation.Path path) {
    String result = path.toString();
    int lastDot = result.lastIndexOf('.');
    return lastDot >= 0 ? result.substring(lastDot + 1) : result;
  }
}
