# Validation Error Implementation Plan (RFC 7807)

## Goal

Replace the custom `ErrorDto` with the RFC 7807 `ProblemDetail` standard so error responses follow a well-known contract. Add a structured `errors` extension containing `field`, `message`, and `code` per violation, enabling the frontend to map server-side errors onto form fields.

## Current State

**Backend** — `ErrorDto` is a custom class with `message` and `Map<String, String> errors`.  
`ConstraintViolationException` handler uses `getMessageTemplate()`, returning raw strings like `{NotBlank.message}`.

**Frontend** — `ErrorDto.errors` is typed as `Record<string, string>` but is never read. Only `errorData.message` is thrown, so field-level server errors are silently discarded.

---

## Target response shape

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "3 constraint(s) violated",
  "errors": [
    { "field": "formKey", "message": "Form key is required", "code": "NotBlank" },
    { "field": "title",   "message": "Title is required",    "code": "NotBlank" }
  ]
}
```

---

## Backend

### 1. Enable RFC 7807 globally
**File:** `backend/src/main/resources/application.properties`

```properties
spring.mvc.problemdetails.enabled=true
```

This makes Spring return `ProblemDetail` for built-in exceptions (`HttpMessageNotReadableException`, `MethodNotAllowedException`, etc.) automatically.

### 2. New `ValidationErrorDto`
**New file:** `backend/src/main/java/com/example/backend/dto/ValidationErrorDto.java`

```java
@Value
public class ValidationErrorDto {
  String field;    // form field name, null for class-level errors
  String message;  // resolved human-readable string
  String code;     // short constraint name, e.g. "NotBlank"
}
```

### 3. Rewrite `GlobalExceptionHandler`
**File:** `backend/src/main/java/com/example/backend/controller/GlobalExceptionHandler.java`

Return `ResponseEntity<ProblemDetail>` from all handlers. Delete `ErrorDto`.

- **`MethodArgumentNotValidException`** — build `ProblemDetail`, add `errors` extension:
  ```java
  List<ValidationErrorDto> errors = ex.getBindingResult().getFieldErrors().stream()
      .map(fe -> new ValidationErrorDto(
          fe.getField(),
          fe.getDefaultMessage(),
          firstCode(fe.getCodes())))
      .toList();
  ProblemDetail pd = ProblemDetail.forStatusAndDetail(BAD_REQUEST, "Validation failed");
  pd.setProperty("errors", errors);
  ```
  `firstCode` strips the fully-qualified code to the short name (e.g. `"NotBlank.formDto.formKey"` → `"NotBlank"`).

- **`ConstraintViolationException`** — same pattern:
  ```java
  List<ValidationErrorDto> errors = ex.getConstraintViolations().stream()
      .map(cv -> new ValidationErrorDto(
          leafPath(cv.getPropertyPath()),
          cv.getMessage(),                   // resolved string, not template
          cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()))
      .toList();
  ```

- **All other handlers** — replace `ErrorDto.of(ex)` with:
  ```java
  ProblemDetail.forStatusAndDetail(status, ex.getMessage())
  ```

### 4. Delete `ErrorDto`
**File:** `backend/src/main/java/com/example/backend/dto/ErrorDto.java` — remove entirely.

---

## Frontend

### 5. Update types in `http.ts`
**File:** `frontend/src/services/http.ts`

```ts
export interface ValidationError {
  field: string | null;
  message: string;
  code: string;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errors?: ValidationError[];
}

export class ApiValidationError extends Error {
  constructor(message: string, public readonly validationErrors: ValidationError[]) {
    super(message);
  }
}
```

In `http.request`, update the error branch:
```ts
const pd = (await response.json()) as ProblemDetail;
if (pd.errors?.length) {
  throw new ApiValidationError(pd.title ?? pd.detail ?? 'Validation failed', pd.errors);
}
throw new Error(pd.detail ?? pd.title ?? `API request failed: ${response.statusText}`);
```

Remove the old `ErrorDto` interface.

### 6. Wire field errors into the form
**File:** `frontend/src/pages/FormSubmission.tsx`

In the mutation's `onError` callback:
```ts
onError: (err) => {
  if (err instanceof ApiValidationError) {
    err.validationErrors.forEach(({field, message}) => {
      if (field) setError(field, {message});
    });
  }
}
```

Existing `<Form.Control.Feedback>` components already render react-hook-form field errors — no new UI needed.

---

## Files Changed

| File | Change |
|------|--------|
| `backend/src/main/resources/application.properties` | Add `spring.mvc.problemdetails.enabled=true` |
| `backend/.../dto/ValidationErrorDto.java` | **New** |
| `backend/.../dto/ErrorDto.java` | **Delete** |
| `backend/.../controller/GlobalExceptionHandler.java` | Return `ProblemDetail` from all handlers |
| `frontend/src/services/http.ts` | Replace `ErrorDto` with `ProblemDetail` + `ApiValidationError` |
| `frontend/src/pages/FormSubmission.tsx` | Handle `ApiValidationError` in mutation `onError` |

---

## Verification

1. `mvn test` — update `GlobalExceptionHandler` tests to assert `ProblemDetail` shape.
2. POST `/api/forms` with blank `formKey` → response matches RFC 7807 shape with `errors` array.
3. Submit the contact form in the browser with a required field empty → server error appears inline under the field.
4. `mvn verify` — all 3 `FrontendIT` Playwright tests still pass.
