# Backend Unit Test Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Raise backend unit test line coverage from 25% to a level where every service is close to fully covered, measured by a JaCoCo report (no build-failing threshold), and fix the defects that the new tests expose.

**Architecture:** Add JaCoCo to the backend build so coverage is measured on every `test` run. Cover services with plain Mockito tests, pure helpers and MapStruct mappers with plain JUnit tests, and controllers plus `GlobalExceptionHandler` with `@WebMvcTest` slice tests that load the real `SecurityConfig`. Four production defects found while analysing coverage are fixed test-first in Phase 4, because the controller tests in Phase 5 depend on the corrected behaviour.

**Tech Stack:** Java 21, Spring Boot 4.0 (Spring Framework 7, Spring Security 7), Jackson 3 (`tools.jackson`), JUnit 5, Mockito, MapStruct, Lombok, Maven, JaCoCo 0.8.14.

**Spec:** No separate spec. Request: "create a phased plan for improving unit test coverage on backend. Service should have close to full line coverage." Analysis is from the session of 2026-10-04 and is summarised under "Baseline" below.

## Global Constraints

- Run Maven from the repository root with `-pl backend -am`. Running `mvn` inside `backend/` can report `BUILD SUCCESS` without running any tests.
- Single test class: `mvn -pl backend -am test -Dtest=<ClassName> -Dsurefire.failIfNoSpecifiedTests=false`
- All unit tests: `mvn -pl backend -am test`
- Unit tests only. No Testcontainers, no `@SpringBootTest`. Integration tests (`*IT`) stay as they are.
- Service tests: `@ExtendWith(MockitoExtension.class)`, `@Mock` dependencies, `@InjectMocks` service, JUnit `Assertions` (not AssertJ). Match the existing tests in `backend/src/test/java/com/example/backend/service/`.
- Controller tests: `@WebMvcTest(<Controller>.class)` + `@MockitoBean`. From Task 6 on they also carry `@Import(SecurityConfig.class)` and `@MockitoBean private JwtDecoder jwtDecoder;`.
- Google Java Style, 2-space indent, as in the existing code.
- Commit messages: one line, imperative, semicolons between concerns, capitalised, no trailing period (see `.claude/CLAUDE.md`).
- Do not commit anything outside the files listed in the task.

## Baseline (measured 2026-10-04, 22 unit tests)

| Class | Line coverage |
|---|---|
| `service.FormService` | 8/17 (47%) |
| `service.FormDataService` | 13/20 (65%) |
| `util.SecurityUtils` | 11/15 (73%) |
| `controller.FormController` | 2/9 (22%) |
| `controller.FormDataController` | 13/17 (76%) |
| `controller.GlobalExceptionHandler` | 2/45 (4%) |
| `config.SecurityConfig` | 0/26 (0%) |
| `mapper.FormMapperImpl` | 0/79 (0%) |
| `mapper.FieldMapperImpl` | 0/47 (0%), unused |
| `mapper.FormDataMapperImpl` | 0/35 (0%) |
| **Total** | **90/360 (25%)** |

## Defects found during analysis (fixed in Phase 4)

1. **Access denied returns 500.** With `SecurityConfig` loaded, a non-admin `DELETE /api/forms/{key}` returns 500: `GlobalExceptionHandler` has no `AccessDeniedException` handler, so the catch-all `Exception` handler answers "Internal server error". Existing slice tests miss this because `@WebMvcTest` does not load `SecurityConfig`, so `@PreAuthorize` is not enforced there at all.
2. **`FormDto` cannot be deserialized.** `@Value @Builder` makes the all-args constructor package-private, so Jackson finds no creator. `POST /api/forms` and `PUT /api/forms/{key}` fail with 500 "Internal server error". The frontend calls `PUT` from `formClient.updateForm`. The same applies to `FieldDto` and `FieldOptionDto`.
3. **Bean Validation does nothing.** Only `jakarta.validation-api` is on the classpath, with no provider (Hibernate Validator). `@Valid` on controllers and `@Validated` on `FormDataService` are ignored.
4. **`GlobalExceptionHandler.handleValidationException` and `handleNoResourceFoundException` never run.** `spring.mvc.problemdetails.enabled=true` registers Spring Boot's own `ResponseEntityExceptionHandler`, which Spring tries first for those exception types. Once validation works, invalid bodies get "Invalid request content." with no `errors` array, and the frontend cannot show field errors. The unused `firstCode()` helper would also return `NotBlank.formDto` instead of `NotBlank`.

## Decisions (2026-10-04)

- **No coverage gate.** JaCoCo only reports; the build never fails on a coverage threshold.
- **Admin delete stays owner-only.** `DELETE /api/form-data/submission/{id}` is admin-only, and `FormDataService.deleteFormSubmission` also requires the caller to own the submission, so an admin can only delete their own submissions. Keep it. Task 3 `deleteFormSubmissionByOtherUserThrowsAndDoesNotDelete` pins it.
- **Omitted `required` defaults to `false`.** Jackson 3 rejects a missing primitive `boolean` (`FAIL_ON_NULL_FOR_PRIMITIVES`), so today a field without `"required"` gets 400 "Failed to read request". Task 7 turns that feature off so a missing primitive gets its default value, as Jackson 2 did.

## Review Focus

- Non-admin calling an admin-only endpoint gets 403, not 500. Test: Task 6 `deleteFormAsNonAdminReturnsForbidden`, Task 9 `deleteSubmissionAsNonAdminReturnsForbidden`.
- Invalid form definition gets 400 with a per-field `errors` array whose `code` is the bare constraint name. Test: Task 7 `createFormWithInvalidBodyReturnsFieldErrors`.
- A field sent without `"required"` is accepted and stored as not required. Test: Task 7 `createFormWithoutRequiredDefaultsToFalse`.
- `updateForm` never changes `formKey` or `id`, even if the request body carries a different key. Test: Task 2 `updateFormCopiesEditableFieldsAndKeepsKey`.
- A user viewing someone else's submission gets 403; a missing submission gets 404. Test: Task 9 `getSubmissionByIdOfOtherUserReturnsForbidden`, `getSubmissionByIdMissingReturnsNotFound`.
- A non-admin listing submissions sees only their own. Test: Task 9 `getSubmissionsAsUserReturnsOnlyOwnSubmissions`.

---

## Phase 1 — Measure

### Task 1: Add JaCoCo coverage reporting

**Files:**
- Modify: `backend/pom.xml:174` (insert after the `maven-failsafe-plugin` `</plugin>`)

**Interfaces:**
- Produces: `backend/target/site/jacoco/index.html` and `backend/target/site/jacoco/jacoco.csv` after every `mvn -pl backend -am test`. Later tasks read `jacoco.csv`.

- [ ] **Step 1: Add the plugin**

Insert after line 174 (`</plugin>` closing `maven-failsafe-plugin`):

```xml
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.14</version>
        <executions>
          <execution>
            <id>prepare-agent</id>
            <goals>
              <goal>prepare-agent</goal>
            </goals>
          </execution>
          <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
              <goal>report</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
```

- [ ] **Step 2: Run the tests and check the report exists**

Run: `mvn -pl backend -am test`
Expected: `Tests run: 22, Failures: 0`, `BUILD SUCCESS`, and `backend/target/site/jacoco/jacoco.csv` exists.

- [ ] **Step 3: Print per-class line coverage**

Run (Git Bash, from repo root):

```bash
awk -F, 'NR>1 {t=$8+$9; printf "%3d%%  %3d/%3d  %s.%s\n", (t?100*$9/t:100), $9, t, $2, $3}' backend/target/site/jacoco/jacoco.csv | sed 's/com.example.backend.//' | sort -k3
```

Expected: matches the Baseline table (total 90/360).

- [ ] **Step 4: Commit**

```bash
git add backend/pom.xml
git commit -m "Add JaCoCo coverage report to backend unit test run"
```

---

## Phase 2 — Services to full coverage

### Task 2: Cover remaining `FormService` paths

**Files:**
- Test: `backend/src/test/java/com/example/backend/service/FormServiceTest.java`

**Interfaces:**
- Consumes: `FormService.updateForm(String formKey, Form updatedDefinition): Form`, `FormService.deleteForm(String formKey): void`, `FormRepository.findByFormKey(String): Optional<Form>`, `FormRepository.save(Form): Form`, `FormRepository.delete(Form): void`.

These tests describe behaviour that already exists, so they pass on the first run. To confirm a test checks something, temporarily break the line it covers (for example comment out `existing.setTitle(...)`), see it fail, then restore.

- [ ] **Step 1: Add the tests**

Add to `FormServiceTest` (the needed imports `Field`, `List`, `Optional` are already present):

```java
  @Test
  void updateFormCopiesEditableFieldsAndKeepsKey() {
    Form existing =
        Form.builder()
            .id(1L)
            .formKey("form1")
            .title("Old")
            .description("Old description")
            .fields(List.of())
            .build();
    List<Field> newFields = List.of(Field.builder().name("field1").type("text").build());
    Form update =
        Form.builder()
            .formKey("other-key")
            .title("New")
            .description("New description")
            .fields(newFields)
            .build();
    when(formRepository.findByFormKey("form1")).thenReturn(Optional.of(existing));
    when(formRepository.save(existing)).thenReturn(existing);

    Form result = formService.updateForm("form1", update);

    assertEquals(1L, result.getId());
    assertEquals("form1", result.getFormKey());
    assertEquals("New", result.getTitle());
    assertEquals("New description", result.getDescription());
    assertEquals(newFields, result.getFields());
    verify(formRepository).save(existing);
  }

  @Test
  void deleteFormDeletesExistingForm() {
    Form existing = Form.builder().id(1L).formKey("form1").build();
    when(formRepository.findByFormKey("form1")).thenReturn(Optional.of(existing));

    formService.deleteForm("form1");

    verify(formRepository).delete(existing);
  }
```

- [ ] **Step 2: Run the tests**

Run: `mvn -pl backend -am test -Dtest=FormServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 9, Failures: 0`

- [ ] **Step 3: Check coverage**

Run the awk command from Task 1 Step 3.
Expected: `service.FormService` at 17/17 (100%).

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/example/backend/service/FormServiceTest.java
git commit -m "Cover FormService update and delete success paths; check updateForm keeps formKey and id"
```

### Task 3: Cover remaining `FormDataService` paths

**Files:**
- Test: `backend/src/test/java/com/example/backend/service/FormDataServiceTest.java`

**Interfaces:**
- Consumes: `FormDataService.createFormSubmission(String, FormData): FormData`, `updateFormSubmission(Long, Map<String,Object>, String): FormData`, `deleteFormSubmission(Long, String): void`, `getFormSubmissionsByOwner(String): List<FormData>`; `FormDataRepository.findBySubmittedByOrderBySubmittedAtDesc(String): List<FormData>`; `FormService.getForm(String): Form` (mocked).

Same note as Task 2: these pass on the first run.

- [ ] **Step 1: Add the tests**

Add to `FormDataServiceTest` (`NoSuchElementException`, `List`, `Map`, `Optional` are already imported):

```java
  @Test
  void createFormSubmissionWithUnknownFormThrowsAndDoesNotSave() {
    FormData formData = new FormData("unknown", Map.of(), "username");
    when(formService.getForm("unknown"))
        .thenThrow(new NoSuchElementException("Form not found: unknown"));

    assertThrows(
        NoSuchElementException.class,
        () -> formDataService.createFormSubmission("unknown", formData));
    verify(formDataRepository, never()).save(any());
  }

  @Test
  void updateFormSubmissionByOwnerReplacesData() {
    FormData existing = new FormData("form1", Map.of("field", "old"), "username");
    Map<String, Object> newData = Map.of("field", "new");
    when(formDataRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(formDataRepository.save(existing)).thenReturn(existing);

    FormData result = formDataService.updateFormSubmission(1L, newData, "username");

    assertEquals(newData, result.getData());
    verify(formDataRepository).save(existing);
  }

  @Test
  void updateFormSubmissionByOtherUserThrowsAndDoesNotSave() {
    when(formDataRepository.findById(1L))
        .thenReturn(Optional.of(new FormData("form1", Map.of(), "owner")));

    assertThrows(
        SecurityException.class,
        () -> formDataService.updateFormSubmission(1L, Map.of(), "intruder"));
    verify(formDataRepository, never()).save(any());
  }

  @Test
  void deleteFormSubmissionByOtherUserThrowsAndDoesNotDelete() {
    when(formDataRepository.findById(1L))
        .thenReturn(Optional.of(new FormData("form1", Map.of(), "owner")));

    assertThrows(
        SecurityException.class, () -> formDataService.deleteFormSubmission(1L, "intruder"));
    verify(formDataRepository, never()).deleteById(any());
  }

  @Test
  void getFormSubmissionsByOwnerReturnsOwnersSubmissions() {
    FormData formData = new FormData("form1", Map.of(), "username");
    when(formDataRepository.findBySubmittedByOrderBySubmittedAtDesc("username"))
        .thenReturn(List.of(formData));

    assertEquals(List.of(formData), formDataService.getFormSubmissionsByOwner("username"));
  }
```

- [ ] **Step 2: Run the tests**

Run: `mvn -pl backend -am test -Dtest=FormDataServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 11, Failures: 0`

- [ ] **Step 3: Check coverage**

Run the awk command from Task 1 Step 3.
Expected: `service.FormDataService` at 20/20 (100%).

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/example/backend/service/FormDataServiceTest.java
git commit -m "Cover FormDataService owner checks, unknown form on create, update success and owner listing"
```

---

## Phase 3 — Helpers and mappers

### Task 4: Test `SecurityUtils`

**Files:**
- Create: `backend/src/test/java/com/example/backend/util/SecurityUtilsTest.java`

**Interfaces:**
- Consumes: `SecurityUtils.getUsername(Jwt): String`, `SecurityUtils.isAdmin(Authentication): boolean`.

- [ ] **Step 1: Write the test class**

```java
package com.example.backend.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityUtilsTest {

  private static Jwt.Builder jwt() {
    return Jwt.withTokenValue("token").header("alg", "none");
  }

  @Test
  void getUsernamePrefersPreferredUsernameClaim() {
    Jwt token = jwt().claim("preferred_username", "alice").subject("subject-1").build();

    assertEquals("alice", SecurityUtils.getUsername(token));
  }

  @Test
  void getUsernameFallsBackToSubject() {
    Jwt token = jwt().subject("subject-1").build();

    assertEquals("subject-1", SecurityUtils.getUsername(token));
  }

  @Test
  void getUsernameReturnsAnonymousForMissingJwt() {
    assertEquals("anonymous", SecurityUtils.getUsername(null));
  }

  @Test
  void isAdminIsTrueForRoleAdmin() {
    assertTrue(SecurityUtils.isAdmin(new TestingAuthenticationToken("user", null, "ROLE_ADMIN")));
  }

  @Test
  void isAdminIsFalseForOtherRoles() {
    assertFalse(SecurityUtils.isAdmin(new TestingAuthenticationToken("user", null, "ROLE_USER")));
  }

  @Test
  void isAdminIsFalseForMissingAuthentication() {
    assertFalse(SecurityUtils.isAdmin(null));
  }

  @Test
  void isAdminIsFalseWhenAuthoritiesAreNull() {
    Authentication authentication = mock(Authentication.class);
    doReturn(null).when(authentication).getAuthorities();

    assertFalse(SecurityUtils.isAdmin(authentication));
  }
}
```

- [ ] **Step 2: Run the tests**

Run: `mvn -pl backend -am test -Dtest=SecurityUtilsTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 7, Failures: 0`

- [ ] **Step 3: Check coverage**

Run the awk command from Task 1 Step 3.
Expected: `util.SecurityUtils` at 14/15 or better (the implicit constructor of the static utility class may stay uncovered).

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/example/backend/util/SecurityUtilsTest.java
git commit -m "Add SecurityUtils tests for username fallback and admin role detection"
```

### Task 5: Test mappers; delete unused `FieldMapper`

**Files:**
- Create: `backend/src/test/java/com/example/backend/mapper/FormMapperTest.java`
- Create: `backend/src/test/java/com/example/backend/mapper/FormDataMapperTest.java`
- Delete: `backend/src/main/java/com/example/backend/mapper/FieldMapper.java`

**Interfaces:**
- Consumes: `FormMapper.toDto(Form): FormDto`, `FormMapper.toEntity(FormDto): Form`, `FormDataMapper.toDto(FormData): FormDataDto`, `FormDataMapper.toEntity(FormDataDto): FormData`, `FormDataMapper.mapList(List<FormData>): List<FormDataDto>`. Mappers are created with `Mappers.getMapper(...)`; no Spring context.

`FieldMapper` is not referenced anywhere (`FormMapper` generates its own field mapping because it has no `uses`). Deleting it removes 47 uncovered generated lines.

- [ ] **Step 1: Confirm `FieldMapper` is unused**

Run: `grep -rn "FieldMapper" backend/src --include=*.java | grep -v "mapper/FieldMapper.java"`
Expected: no output. If anything is printed, stop and keep the file.

- [ ] **Step 2: Write `FormMapperTest`**

```java
package com.example.backend.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.example.backend.dto.FieldDto;
import com.example.backend.dto.FormDto;
import com.example.backend.entity.Field;
import com.example.backend.entity.FieldOption;
import com.example.backend.entity.Form;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class FormMapperTest {

  private final FormMapper formMapper = Mappers.getMapper(FormMapper.class);

  private static Form form() {
    return Form.builder()
        .id(1L)
        .formKey("form1")
        .title("Title")
        .description("Description")
        .fields(
            List.of(
                Field.builder()
                    .name("color")
                    .label("Color")
                    .type("select")
                    .required(true)
                    .placeholder("Pick one")
                    .options(List.of(FieldOption.builder().value("red").label("Red").build()))
                    .build()))
        .build();
  }

  @Test
  void toDtoCopiesFormFieldsAndOptions() {
    FormDto dto = formMapper.toDto(form());

    assertEquals(1L, dto.getId());
    assertEquals("form1", dto.getFormKey());
    assertEquals("Title", dto.getTitle());
    assertEquals("Description", dto.getDescription());
    FieldDto field = dto.getFields().getFirst();
    assertEquals("color", field.getName());
    assertEquals("Color", field.getLabel());
    assertEquals("select", field.getType());
    assertTrue(field.isRequired());
    assertEquals("Pick one", field.getPlaceholder());
    assertEquals("red", field.getOptions().getFirst().getValue());
    assertEquals("Red", field.getOptions().getFirst().getLabel());
  }

  @Test
  void toEntityReversesToDto() {
    Form original = form();

    Form result = formMapper.toEntity(formMapper.toDto(original));

    assertEquals(original.getId(), result.getId());
    assertEquals(original.getFormKey(), result.getFormKey());
    assertEquals(original.getTitle(), result.getTitle());
    assertEquals(original.getDescription(), result.getDescription());
    assertEquals(original.getFields(), result.getFields());
  }

  @Test
  void nullMapsToNull() {
    assertNull(formMapper.toDto(null));
    assertNull(formMapper.toEntity(null));
  }
}
```

- [ ] **Step 3: Write `FormDataMapperTest`**

```java
package com.example.backend.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.example.backend.dto.FormDataDto;
import com.example.backend.entity.FormData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class FormDataMapperTest {

  private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 1, 2, 3, 4);

  private final FormDataMapper formDataMapper = Mappers.getMapper(FormDataMapper.class);

  @Test
  void toDtoCopiesAllFields() {
    FormData entity = new FormData(5L, "form1", Map.of("field", "value"), SUBMITTED_AT, "user");

    assertEquals(
        new FormDataDto(5L, "form1", Map.of("field", "value"), SUBMITTED_AT, "user"),
        formDataMapper.toDto(entity));
  }

  @Test
  void toEntityCopiesAllFields() {
    FormDataDto dto = new FormDataDto(5L, "form1", Map.of("field", "value"), SUBMITTED_AT, "user");

    assertEquals(
        new FormData(5L, "form1", Map.of("field", "value"), SUBMITTED_AT, "user"),
        formDataMapper.toEntity(dto));
  }

  @Test
  void mapListMapsEachElementInOrder() {
    List<FormData> entities =
        List.of(
            new FormData(1L, "form1", Map.of(), SUBMITTED_AT, "user"),
            new FormData(2L, "form2", Map.of(), SUBMITTED_AT, "user"));

    List<FormDataDto> result = formDataMapper.mapList(entities);

    assertEquals(List.of(1L, 2L), result.stream().map(FormDataDto::getId).toList());
  }

  @Test
  void nullMapsToNull() {
    assertNull(formDataMapper.toDto(null));
    assertNull(formDataMapper.toEntity(null));
    assertNull(formDataMapper.mapList(null));
  }
}
```

- [ ] **Step 4: Delete `FieldMapper`**

```bash
git rm backend/src/main/java/com/example/backend/mapper/FieldMapper.java
```

- [ ] **Step 5: Run all unit tests**

Run: `mvn -pl backend -am test`
Expected: `BUILD SUCCESS`, no failures. `mapper.FieldMapperImpl` no longer appears in the coverage output; `FormMapperImpl` and `FormDataMapperImpl` above 80%. MapStruct null-checks on nested collections may leave a few branches uncovered; do not chase them.

- [ ] **Step 6: Commit**

```bash
git add backend/src/test/java/com/example/backend/mapper/FormMapperTest.java backend/src/test/java/com/example/backend/mapper/FormDataMapperTest.java
git commit -m "Add FormMapper and FormDataMapper tests; remove unused FieldMapper"
```

---

## Phase 4 — Fix defects the tests expose

### Task 6: Enforce security in controller slice tests; return 403 for access denied

**Files:**
- Modify: `backend/src/main/java/com/example/backend/controller/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/example/backend/controller/FormControllerTest.java`
- Test: `backend/src/test/java/com/example/backend/controller/FormDataControllerTest.java`

**Interfaces:**
- Produces: `GlobalExceptionHandler.handleAccessDeniedException(AccessDeniedException): ResponseEntity<ProblemDetail>` returning 403. Both controller test classes now load `SecurityConfig` with a mocked `JwtDecoder`; Tasks 8–10 rely on this.

- [ ] **Step 1: Load `SecurityConfig` in both controller tests**

In `FormControllerTest` and `FormDataControllerTest`, add the annotation and field:

```java
@WebMvcTest(FormController.class) // FormDataController.class in the other file
@Import(SecurityConfig.class)
class FormControllerTest {

  @MockitoBean private JwtDecoder jwtDecoder;
```

Imports:

```java
import com.example.backend.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
```

- [ ] **Step 2: Run the existing controller tests**

Run: `mvn -pl backend -am test -Dtest='FormControllerTest,FormDataControllerTest' -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 5, Failures: 0`. The existing tests already authenticate with the right roles.

- [ ] **Step 3: Write the failing test**

Add to `FormControllerTest`:

```java
  @Test
  void deleteFormAsNonAdminReturnsForbidden() throws Exception {
    mockMvc.perform(delete("/api/forms/form1").with(jwt())).andExpect(status().isForbidden());

    verify(formService, never()).deleteForm(any());
  }
```

Imports:

```java
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
```

- [ ] **Step 4: Run it to verify it fails**

Run: `mvn -pl backend -am test -Dtest=FormControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL with `Status expected:<403> but was:<500>`

- [ ] **Step 5: Add the handler**

In `GlobalExceptionHandler`, after `handleSecurityException`:

```java
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex) {
    log.warn("AccessDeniedException: {}", ex.getMessage());
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }
```

Import: `import org.springframework.security.access.AccessDeniedException;`

- [ ] **Step 6: Run the tests**

Run: `mvn -pl backend -am test`
Expected: `BUILD SUCCESS`, `FormControllerTest` 2 tests passing.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/example/backend/controller/GlobalExceptionHandler.java backend/src/test/java/com/example/backend/controller/FormControllerTest.java backend/src/test/java/com/example/backend/controller/FormDataControllerTest.java
git commit -m "Return 403 instead of 500 when method security denies access; load SecurityConfig in controller slice tests so @PreAuthorize is enforced"
```

### Task 7: Make form definitions deserializable and validated; return field errors

**Files:**
- Modify: `backend/pom.xml:56-60` (validation dependency)
- Modify: `backend/src/main/java/com/example/backend/dto/FormDto.java`
- Modify: `backend/src/main/java/com/example/backend/dto/FieldDto.java`
- Modify: `backend/src/main/java/com/example/backend/dto/FieldOptionDto.java`
- Modify: `backend/src/main/java/com/example/backend/controller/GlobalExceptionHandler.java`
- Modify: `backend/src/main/resources/application.properties`
- Test: `backend/src/test/java/com/example/backend/controller/FormControllerTest.java`

**Interfaces:**
- Consumes: `FormControllerTest` with `SecurityConfig` and `JwtDecoder` (Task 6).
- Produces: `GlobalExceptionHandler extends ResponseEntityExceptionHandler`, overriding `handleMethodArgumentNotValid(MethodArgumentNotValidException, HttpHeaders, HttpStatusCode, WebRequest): ResponseEntity<Object>`. Response body: `ProblemDetail` with `detail = "Validation failed"` and property `errors: List<ValidationErrorDto>`; `ValidationErrorDto.code` is the bare constraint name (`NotBlank`, `Pattern`). `handleValidationException`, `handleNoResourceFoundException` and `firstCode` are removed. Constants `VALID_FORM_JSON` and helper `adminJwt()` in `FormControllerTest` are used again in Task 8.

- [ ] **Step 1: Write the failing tests**

Add to `FormControllerTest`:

```java
  private static final String VALID_FORM_JSON =
      """
      {"formKey":"form1","title":"Test Form","fields":[
        {"name":"field1","label":"Field 1","type":"text","required":true}]}
      """;

  private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  @Test
  void createFormWithValidBodyReturnsCreated() throws Exception {
    Form form = Form.builder().formKey("form1").title("Test Form").build();
    when(formService.existsByFormKey("form1")).thenReturn(false);
    when(formMapper.toEntity(any(FormDto.class))).thenReturn(form);
    when(formService.saveForm(form)).thenReturn(form);
    when(formMapper.toDto(form)).thenReturn(FormDto.builder().formKey("form1").build());

    mockMvc
        .perform(
            post("/api/forms")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_FORM_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.formKey").value("form1"));
  }

  @Test
  void createFormWithInvalidBodyReturnsFieldErrors() throws Exception {
    String body =
        """
        {"formKey":"Bad Key","title":"","fields":[
          {"name":"field1","label":"Field 1","type":"text","required":false}]}
        """;

    mockMvc
        .perform(
            post("/api/forms")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"))
        .andExpect(jsonPath("$.errors[?(@.field == 'formKey')].code").value("Pattern"))
        .andExpect(jsonPath("$.errors[?(@.field == 'title')].code").value("NotBlank"));

    verify(formService, never()).saveForm(any());
  }

  @Test
  void createFormWithoutRequiredDefaultsToFalse() throws Exception {
    String body =
        """
        {"formKey":"form1","title":"Test Form","fields":[
          {"name":"field1","label":"Field 1","type":"text"}]}
        """;
    Form form = Form.builder().formKey("form1").title("Test Form").build();
    when(formMapper.toEntity(any(FormDto.class))).thenReturn(form);
    when(formService.saveForm(form)).thenReturn(form);
    when(formMapper.toDto(form)).thenReturn(FormDto.builder().formKey("form1").build());

    mockMvc
        .perform(
            post("/api/forms")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());

    ArgumentCaptor<FormDto> captor = ArgumentCaptor.forClass(FormDto.class);
    verify(formMapper).toEntity(captor.capture());
    assertFalse(captor.getValue().getFields().getFirst().isRequired());
  }
```

Imports:

```java
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
```

- [ ] **Step 2: Run them to verify they fail**

Run: `mvn -pl backend -am test -Dtest=FormControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: all three FAIL: `Status expected:<201> but was:<500>` (twice) and `Status expected:<400> but was:<500>`. The log shows `Cannot construct instance of com.example.backend.dto.FormDto (no Creators ...)`.

- [ ] **Step 3: Give the DTOs a public constructor**

In `FormDto`, `FieldDto` and `FieldOptionDto`, add `@AllArgsConstructor` under `@Builder` and import it:

```java
import lombok.AllArgsConstructor;

@Value
@Builder
@AllArgsConstructor
public class FormDto {
```

- [ ] **Step 4: Add the Bean Validation provider**

Replace `backend/pom.xml:56-60`:

```xml
    <dependency>
      <groupId>jakarta.validation</groupId>
      <artifactId>jakarta.validation-api</artifactId>
      <version>3.1.1</version>
    </dependency>
```

with:

```xml
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
```

The starter brings `jakarta.validation-api` (version managed by Spring Boot) and Hibernate Validator.

- [ ] **Step 5: Run the tests again**

Run: `mvn -pl backend -am test -Dtest=FormControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `createFormWithValidBodyReturnsCreated` PASSES. Two still FAIL:
- `createFormWithoutRequiredDefaultsToFalse`: `Status expected:<201> but was:<400>`; the log shows `Cannot map null into type boolean`.
- `createFormWithInvalidBodyReturnsFieldErrors`: status is 400 but `$.detail` is `Invalid request content.` and there is no `errors` array, because Spring Boot's ProblemDetail handler answers first.

- [ ] **Step 5b: Let missing primitives take their default value**

Add to `backend/src/main/resources/application.properties`:

```properties
spring.jackson.deserialization.fail-on-null-for-primitives=false
```

Run: `mvn -pl backend -am test -Dtest=FormControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `createFormWithoutRequiredDefaultsToFalse` PASSES; only `createFormWithInvalidBodyReturnsFieldErrors` still fails. If the property has no effect, check the Spring Boot 4 property name for Jackson 3 deserialization features before trying anything else.

- [ ] **Step 6: Take over validation errors in `GlobalExceptionHandler`**

Make the class extend `ResponseEntityExceptionHandler`. Spring Boot then skips registering its own one (it is conditional on no `ResponseEntityExceptionHandler` bean), and the base class handles the standard Spring MVC exceptions, including `NoResourceFoundException` → 404.

Replace `handleValidationException` with this override, and delete `handleNoResourceFoundException` and `firstCode`:

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    log.warn("Validation failed: {}", ex.getMessage());
    List<ValidationErrorDto> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ValidationErrorDto(fe.getField(), fe.getDefaultMessage(), fe.getCode()))
            .toList();
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    pd.setProperty("errors", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }
```

`FieldError.getCode()` returns the last, bare code (`NotBlank`); the old `firstCode(fe.getCodes())` returned `NotBlank.formDto`.

Import changes:

```java
// add
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
// remove
import org.springframework.web.servlet.resource.NoResourceFoundException;
```

- [ ] **Step 7: Run all unit tests**

Run: `mvn -pl backend -am test`
Expected: `BUILD SUCCESS`, no failures.

- [ ] **Step 8: Run the integration tests**

The validation provider also activates `@Validated` on `FormDataService` and validation in the full application, so check the end-to-end path once. Docker must be running.

Run: `mvn -pl backend -am verify`
Expected: `BUILD SUCCESS`, `FrontendIT` 3 tests passing.

- [ ] **Step 9: Commit**

```bash
git add backend/pom.xml backend/src/main/java/com/example/backend/dto/FormDto.java backend/src/main/java/com/example/backend/dto/FieldDto.java backend/src/main/java/com/example/backend/dto/FieldOptionDto.java backend/src/main/java/com/example/backend/controller/GlobalExceptionHandler.java backend/src/main/resources/application.properties backend/src/test/java/com/example/backend/controller/FormControllerTest.java
git commit -m "Fix form create and update failing with 500: give DTOs a public constructor for Jackson; add Hibernate Validator so @Valid runs; return field errors from GlobalExceptionHandler instead of Spring Boot's default handler; default omitted required to false"
```

---

## Phase 5 — Controllers and exception handler

### Task 8: Cover remaining `FormController` endpoints

**Files:**
- Test: `backend/src/test/java/com/example/backend/controller/FormControllerTest.java`

**Interfaces:**
- Consumes: `VALID_FORM_JSON`, `adminJwt()` (Task 7); `FormService.getForms(): List<FormListItemDto>`, `getForm(String): Form`, `existsByFormKey(String): boolean`, `updateForm(String, Form): Form`, `deleteForm(String): void`.

- [ ] **Step 1: Add the tests**

```java
  @Test
  void getFormsReturnsListItems() throws Exception {
    when(formService.getForms()).thenReturn(List.of(new FormListItemDto("form1", "Form 1")));

    mockMvc
        .perform(get("/api/forms").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].formKey").value("form1"))
        .andExpect(jsonPath("$[0].title").value("Form 1"));
  }

  @Test
  void getFormsWithoutAuthenticationReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/forms")).andExpect(status().isUnauthorized());
  }

  @Test
  void getFormMissingReturnsNotFound() throws Exception {
    when(formService.getForm("missing"))
        .thenThrow(new NoSuchElementException("Form not found: missing"));

    mockMvc
        .perform(get("/api/forms/missing").with(jwt()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Form not found: missing"));
  }

  @Test
  void createFormWithExistingKeyReturnsBadRequest() throws Exception {
    when(formService.existsByFormKey("form1")).thenReturn(true);

    mockMvc
        .perform(
            post("/api/forms")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_FORM_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Form with key 'form1' already exists"));

    verify(formService, never()).saveForm(any());
  }

  @Test
  void updateFormAsAdminReturnsUpdatedForm() throws Exception {
    Form form = Form.builder().formKey("form1").title("Test Form").build();
    when(formMapper.toEntity(any(FormDto.class))).thenReturn(form);
    when(formService.updateForm("form1", form)).thenReturn(form);
    when(formMapper.toDto(form)).thenReturn(FormDto.builder().formKey("form1").build());

    mockMvc
        .perform(
            put("/api/forms/form1")
                .with(adminJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_FORM_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.formKey").value("form1"));
  }

  @Test
  void deleteFormAsAdminReturnsNoContent() throws Exception {
    mockMvc
        .perform(delete("/api/forms/form1").with(adminJwt()))
        .andExpect(status().isNoContent());

    verify(formService).deleteForm("form1");
  }
```

Imports:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.example.backend.dto.FormListItemDto;
import java.util.NoSuchElementException;
```

- [ ] **Step 2: Run the tests**

Run: `mvn -pl backend -am test -Dtest=FormControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 11, Failures: 0`

- [ ] **Step 3: Check coverage**

Run the awk command from Task 1 Step 3.
Expected: `controller.FormController` at 9/9 (100%).

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/example/backend/controller/FormControllerTest.java
git commit -m "Cover FormController list, not-found, duplicate key, update, delete and unauthenticated access"
```

### Task 9: Cover remaining `FormDataController` paths

**Files:**
- Test: `backend/src/test/java/com/example/backend/controller/FormDataControllerTest.java`

**Interfaces:**
- Consumes: existing `createJwtAuth(String subject, Collection<SimpleGrantedAuthority>)` helper in the test class; `SecurityConfig` loaded (Task 6); `handleAccessDeniedException` (Task 6).

- [ ] **Step 1: Add the tests**

```java
  private static final Collection<SimpleGrantedAuthority> USER =
      Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

  @Test
  void getSubmissionsAsUserReturnsOnlyOwnSubmissions() throws Exception {
    FormData formData = new FormData("form1", Map.of(), "testuser");
    FormDataDto formDataDto =
        new FormDataDto(1L, "form1", Map.of(), LocalDateTime.now(), "testuser");
    when(formDataService.getFormSubmissionsByOwner("testuser")).thenReturn(List.of(formData));
    when(formDataMapper.mapList(List.of(formData))).thenReturn(List.of(formDataDto));

    mockMvc
        .perform(get("/api/form-data").with(authentication(createJwtAuth("testuser", USER))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].submittedBy").value("testuser"));

    verify(formDataService, never()).getFormSubmissions();
  }

  @Test
  void getSubmissionByIdOfOtherUserReturnsForbidden() throws Exception {
    when(formDataService.getFormSubmissionById(1L))
        .thenReturn(Optional.of(new FormData("form1", Map.of(), "owner")));

    mockMvc
        .perform(
            get("/api/form-data/submission/1")
                .with(authentication(createJwtAuth("intruder", USER))))
        .andExpect(status().isForbidden());
  }

  @Test
  void getSubmissionByIdMissingReturnsNotFound() throws Exception {
    when(formDataService.getFormSubmissionById(99L)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            get("/api/form-data/submission/99")
                .with(authentication(createJwtAuth("testuser", USER))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Form submission not found: 99"));
  }

  @Test
  void updateSubmissionPassesCallerAsUsername() throws Exception {
    Map<String, Object> data = Map.of("field1", "new");
    FormData formData = new FormData("form1", data, "testuser");
    FormDataDto formDataDto = new FormDataDto(1L, "form1", data, LocalDateTime.now(), "testuser");
    when(formDataService.updateFormSubmission(1L, data, "testuser")).thenReturn(formData);
    when(formDataMapper.toDto(formData)).thenReturn(formDataDto);

    mockMvc
        .perform(
            put("/api/form-data/submission/1")
                .with(authentication(createJwtAuth("testuser", USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.field1").value("new"));
  }

  @Test
  void deleteSubmissionAsNonAdminReturnsForbidden() throws Exception {
    mockMvc
        .perform(
            delete("/api/form-data/submission/1")
                .with(authentication(createJwtAuth("testuser", USER))))
        .andExpect(status().isForbidden());

    verify(formDataService, never()).deleteFormSubmission(any(), any());
  }
```

Imports:

```java
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
```

- [ ] **Step 2: Run the tests**

Run: `mvn -pl backend -am test -Dtest=FormDataControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 9, Failures: 0`

- [ ] **Step 3: Check coverage**

Run the awk command from Task 1 Step 3.
Expected: `controller.FormDataController` at 17/17 (100%).

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/example/backend/controller/FormDataControllerTest.java
git commit -m "Cover FormDataController owner filtering, forbidden and missing submissions, update and non-admin delete"
```

### Task 10: Unit-test `GlobalExceptionHandler`

**Files:**
- Create: `backend/src/test/java/com/example/backend/controller/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `GlobalExceptionHandler` after Task 7, with public methods `handleIllegalArgumentException`, `handleConstraintViolationException`, `handleNoSuchElementException`, `handleSecurityException`, `handleAccessDeniedException`, `handleIllegalStateException`, `handleGenericException`, each returning `ResponseEntity<ProblemDetail>`. Hibernate Validator on the classpath (Task 7).

- [ ] **Step 1: Write the test class**

```java
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
```

- [ ] **Step 2: Run the tests**

Run: `mvn -pl backend -am test -Dtest=GlobalExceptionHandlerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `Tests run: 7, Failures: 0`

- [ ] **Step 3: Check coverage**

Run the awk command from Task 1 Step 3.
Expected: `controller.GlobalExceptionHandler` above 90%.

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/example/backend/controller/GlobalExceptionHandlerTest.java
git commit -m "Add GlobalExceptionHandler tests for status mapping, hidden 500 details and constraint violation field names"
```

### Task 11: Final verification

**Files:** none changed unless a check fails.

- [ ] **Step 1: Run the full suite**

Run the `/test-all` skill (frontend Vitest + `mvn -pl backend -am verify`).
Expected: all frontend and backend tests pass.

- [ ] **Step 2: Record the new coverage**

Run the awk command from Task 1 Step 3 and the total:

```bash
awk -F, 'NR>1{m+=$8;c+=$9} END{printf "TOTAL lines %d/%d = %d%%\n", c, m+c, 100*c/(m+c)}' backend/target/site/jacoco/jacoco.csv
```

Expected: `service.FormService` and `service.FormDataService` at 100%; total line coverage well above the 25% baseline. Report the per-class table in the hand-off message. Not covered on purpose: `BackendApplication.main`, `ConfigController` (one getter), the `IOException` branch in `DatabaseInitializer`.
