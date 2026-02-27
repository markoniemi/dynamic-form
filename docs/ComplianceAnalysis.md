# Compliance Analysis

Analysis of codebase compliance against [copilot-instructions.md](../.github/copilot-instructions.md).

---

## Summary

| Category | Violations | Status |
|----------|-----------|--------|
| Security (missing @PreAuthorize) | 2 controllers | 🔴 Not started |
| Wrong Exception Type | 2 files | 🔴 Not started |
| DTO immutability | 4 DTOs | 🔴 Not started |
| TypeScript `any` type | 7 instances | 🔴 Not started |
| Frontend file organization | 6 components | 🔴 Not started |
| Test naming convention | 20+ methods | 🔴 Not started |
| Inline styles | 1 instance | 🔴 Not started |
| Java indentation (2-space) | Multiple files | 🔴 Not started |
| Stream API modernization | 3 locations | 🔴 Not started |

---

## High Priority

### 1. Missing `@PreAuthorize` on Controller Endpoints

**Rule**: Controllers should secure endpoints using Spring Security annotations (`@PreAuthorize`).

**Affected files**:
- `backend/src/main/java/com/example/backend/controller/FormController.java`
- `backend/src/main/java/com/example/backend/controller/FormDataController.java`

**Tasks**:
- [ ] Add `@PreAuthorize` to all endpoints in `FormController.java`
  - [ ] `GET /api/forms` — e.g. `@PreAuthorize("isAuthenticated()")`
  - [ ] `GET /api/forms/{formKey}` — e.g. `@PreAuthorize("isAuthenticated()")`
  - [ ] `POST /api/forms` — e.g. `@PreAuthorize("hasRole('ADMIN')")`
  - [ ] `PUT /api/forms/{formKey}` — e.g. `@PreAuthorize("hasRole('ADMIN')")`
  - [ ] `DELETE /api/forms/{formKey}` — e.g. `@PreAuthorize("hasRole('ADMIN')")`
- [ ] Add `@PreAuthorize` to all endpoints in `FormDataController.java`
  - [ ] `POST /api/forms/{formKey}/submissions` — e.g. `@PreAuthorize("isAuthenticated()")`
  - [ ] `GET /api/forms/submissions` — e.g. `@PreAuthorize("isAuthenticated()")`
  - [ ] `GET /api/forms/{formKey}/submissions` — e.g. `@PreAuthorize("isAuthenticated()")`
  - [ ] `GET /api/forms/submissions/{id}` — e.g. `@PreAuthorize("isAuthenticated()")`
  - [ ] `DELETE /api/forms/submissions/{id}` — e.g. `@PreAuthorize("hasRole('ADMIN')")`
- [ ] Enable method security in `SecurityConfig.java` with `@EnableMethodSecurity` if not already present

---

### 2. Wrong Exception Type for "Not Found" Cases

**Rule**: Use `NoSuchElementException` for not-found cases (→ 404) and `IllegalArgumentException` for bad input (→ 400). Map both in `GlobalExceptionHandler`.

**Issue**: `FormService.java` throws `IllegalArgumentException` when a form is not found. `GlobalExceptionHandler` already maps `IllegalArgumentException` → **400 Bad Request**, so not-found cases currently return the wrong HTTP status. There is also no handler for `NoSuchElementException` → **404 Not Found** in `GlobalExceptionHandler`.

**Affected files**:
- `backend/src/main/java/com/example/backend/service/FormService.java` — lines 34, 58, 72 throw `IllegalArgumentException` for not-found cases (should be `NoSuchElementException`)
- `backend/src/main/java/com/example/backend/controller/GlobalExceptionHandler.java` — missing `NoSuchElementException` → 404 handler

**Tasks**:
- [ ] In `FormService.java`, replace `IllegalArgumentException` with `NoSuchElementException` on lines 34, 58, and 72
- [ ] Check `FormDataController.java:60` — if it is also a not-found case, replace with `NoSuchElementException`; if it is truly bad input, `IllegalArgumentException` is correct
- [ ] Add a `NoSuchElementException` handler to `GlobalExceptionHandler.java` returning **404 Not Found**

---

## Medium Priority

### 3. DTO Immutability

**Rule**: Prefer immutability, especially for DTOs. Use Lombok `@Value` annotation where possible.

**Issue**: DTOs currently use `@Data` (which generates mutable setters) or conflicting annotations. MapStruct requires either an all-args constructor (provided by `@Value`) or setters. `@Value` is the preferred approach as it creates immutable records.

> **Note**: `@Value` from Lombok makes all fields `private final` and generates an all-args constructor — it is compatible with MapStruct when using `@Builder` or `@AllArgsConstructor` mapping strategy.

**Affected files**:
- `backend/src/main/java/com/example/backend/dto/FormDto.java`
- `backend/src/main/java/com/example/backend/dto/FieldDto.java`
- `backend/src/main/java/com/example/backend/dto/FieldOptionDto.java`
- `backend/src/main/java/com/example/backend/dto/FormDataDto.java` — also has conflicting `@Data` + Spring `@Value`

**Tasks**:
- [ ] Replace `@Data` with `@Value` on `FormDto.java`
- [ ] Replace `@Data` with `@Value` on `FieldDto.java`
- [ ] Replace `@Data` with `@Value` on `FieldOptionDto.java`
- [ ] Fix `FormDataDto.java`: remove `@Data`, resolve Spring `@Value` conflict, use Lombok `@Value`
- [ ] Verify MapStruct mappers still compile and work correctly after making DTOs immutable
- [ ] Run all tests to confirm no regressions

---

### 4. TypeScript `any` Type

**Rule**: Do not use the `any` type; always strive for precise typing.

**Affected files and locations**:

| File | Location | Issue |
|------|----------|-------|
| `frontend/src/components/CheckboxField.tsx` | Line 9 | `UseFormRegister<any>` |
| `frontend/src/components/DynamicForm.tsx` | Line 12 | `UseFormRegister<any>` |
| `frontend/src/components/RadioField.tsx` | Line 9 | `UseFormRegister<any>` |
| `frontend/src/components/SelectField.tsx` | Line 9 | `UseFormRegister<any>` |
| `frontend/src/components/TextAreaField.tsx` | Line 9 | `UseFormRegister<any>` |
| `frontend/src/pages/FormSubmission.tsx` | Line 35 | `Record<string, any>` |
| `frontend/src/types/Form.ts` | Line 33 | `Record<string, any>` |

**Tasks**:
- [ ] Define a shared form values type (e.g. `FormValues = Record<string, string | string[] | boolean>`) in `types/Form.ts`
- [ ] Replace `UseFormRegister<any>` with `UseFormRegister<FormValues>` in all field components:
  - [ ] `CheckboxField.tsx`
  - [ ] `DynamicForm.tsx`
  - [ ] `RadioField.tsx`
  - [ ] `SelectField.tsx`
  - [ ] `TextAreaField.tsx`
- [ ] Replace `Record<string, any>` in `FormSubmission.tsx` with the proper typed form values type
- [ ] Replace `Record<string, any>` in `types/Form.ts` with the proper typed form values type
- [ ] Fix `(useParams as any)` and `(useAuth as any)` casts in test files with proper mock types

---

### 5. Frontend File Organization

**Rule**: Organize components into subdirectories:
- `components/common/` — Generic reusable components (buttons, inputs, etc.)
- `components/layout/` — Page structure components (Header, Footer, Navigation)

**Current state**: All components are flat in `frontend/src/components/`.

**Affected files**:

| File | Should move to |
|------|---------------|
| `components/Navigation.tsx` | `components/layout/Navigation.tsx` |
| `components/Content.tsx` | `components/layout/Content.tsx` |
| `components/FieldWrapper.tsx` | `components/common/FieldWrapper.tsx` |
| `components/TextField.tsx` | `components/common/TextField.tsx` |
| `components/TextAreaField.tsx` | `components/common/TextAreaField.tsx` |
| `components/CheckboxField.tsx` | `components/common/CheckboxField.tsx` |
| `components/RadioField.tsx` | `components/common/RadioField.tsx` |
| `components/SelectField.tsx` | `components/common/SelectField.tsx` |
| `components/DynamicForm.tsx` | `components/common/DynamicForm.tsx` |
| `components/ReadOnlyDynamicForm.tsx` | `components/common/ReadOnlyDynamicForm.tsx` |
| `components/FieldEditor.tsx` | `components/common/FieldEditor.tsx` |

**Tasks**:
- [ ] Create `frontend/src/components/layout/` directory
- [ ] Create `frontend/src/components/common/` directory
- [ ] Move layout components (`Navigation.tsx`, `Content.tsx`) to `components/layout/`
- [ ] Move generic/reusable components to `components/common/`
- [ ] Update all import paths in files that reference the moved components
- [ ] Verify the app builds and all tests pass after moving files

---

## Low Priority

### 6. Test Naming Convention

**Rule**: Test method names should follow:
- Happy path: `methodUnderTest` (e.g. `withdrawCash`)
- Non-trivial cases: `methodUnderTestWithStateUnderTestExpectedBehavior` (e.g. `withdrawCashWithInsufficientBalanceThrowsException`)

**Affected files**:

#### `FormControllerTest.java`
- [ ] `getAvailableForms` — acceptable as-is (happy path)
- [ ] `getForm` — acceptable as-is (happy path)
- [ ] Ensure negative/error cases follow full naming convention

#### `FormDataControllerTest.java`
- [ ] `submitForm` — acceptable as-is (happy path)
- [ ] `getAllSubmissions` — acceptable as-is (happy path)
- [ ] `getSubmissionsByFormKey` — acceptable as-is (happy path)
- [ ] `getSubmissionById` — acceptable as-is (happy path)
- [ ] `deleteSubmission` — acceptable as-is (happy path)
- [ ] Add/rename any error-case tests to follow full convention (e.g. `getSubmissionByIdWithInvalidIdThrowsException`)

#### `DatabaseInitializerTest.java`
- [ ] `runLoadsFormsFromResources` — rename to `runWithFormsInResourcesLoadsAllForms`
- [ ] `runSkipsExistingForms` — rename to `runWithExistingFormSkipsImport`
- [ ] `runParsesFormCorrectly` — rename to `runWithValidJsonParsesFormCorrectly`

#### `FormDataRepositoryTest.java`
- [ ] `findByFormKeyOrderBySubmittedAtDesc` — rename to `findByFormKeyOrderBySubmittedAtDescWithMatchingKeyReturnsOrderedResults`

#### `FormDataServiceTest.java`
- [ ] `createFormSubmission` — acceptable as-is (happy path)
- [ ] `getFormSubmissionById` — acceptable as-is (happy path)
- [ ] `getAllFormSubmissions` — acceptable as-is (happy path)
- [ ] `getFormSubmissionsByKey` — acceptable as-is (happy path)
- [ ] `deleteFormSubmission` — acceptable as-is (happy path)
- [ ] Add/rename error-case tests to follow full convention

#### `FormServiceTest.java`
- [ ] `getAvailableFormKeys` — acceptable as-is (happy path)
- [ ] `getForm` — acceptable as-is (happy path)
- [ ] `getFormWithNotFoundThrowsException` — ✅ correctly named
- [ ] `saveForm` — acceptable as-is (happy path)
- [ ] `existsByFormKey` — acceptable as-is (happy path)

---

### 7. Inline Styles

**Rule**: Prefer `react-bootstrap` components and utility classes over custom CSS.

**Affected files**:
- `frontend/src/pages/FormSubmissions.tsx` — line 71: `style={{ margin: 0, maxHeight: '100px', overflowY: 'auto' }}`

**Tasks**:
- [ ] Replace inline `margin: 0` with Bootstrap utility class `m-0`
- [ ] Replace inline `maxHeight` + `overflowY` with a Bootstrap utility or a dedicated CSS class in the stylesheet (no Bootstrap equivalent for max-height scroll, a small CSS class is acceptable)

---

### 8. Java Indentation (2-Space)

**Rule**: Google Java Style Guide requires 2-space indentation.

**Affected files**: Most Java files appear to use 4-space indentation.

**Tasks**:
- [ ] Configure IDE/editor to use 2-space indentation for Java files
- [ ] Apply Google Java Format to all Java source files
- [ ] Consider adding a `google-java-format` Maven plugin or `.editorconfig` to enforce this going forward

---

### 9. Stream API Modernization

**Rule**: Follow modern Java conventions (Java 16+ `.toList()` vs `.collect(Collectors.toList())`).

**Affected files**:
- `FormController.java` — line 40
- `FormDataController.java` — lines 44, 52

**Tasks**:
- [ ] Replace `.collect(Collectors.toList())` with `.toList()` in `FormController.java:40`
- [ ] Replace `.collect(Collectors.toList())` with `.toList()` in `FormDataController.java:44`
- [ ] Replace `.collect(Collectors.toList())` with `.toList()` in `FormDataController.java:52`

---

## Already Compliant

The following areas were found to be compliant with the instructions:

- ✅ Lombok used throughout backend
- ✅ Constructor injection used (no field injection)
- ✅ MapStruct mappers exist (`FormMapper`, `FieldMapper`, `FormDataMapper`)
- ✅ DTOs used in API layer — entities not exposed directly
- ✅ Repositories extend `JpaRepository`
- ✅ `@Service`, `@RestController`, `@RequestMapping("/api/...")` correctly applied
- ✅ `@ControllerAdvice` present (`GlobalExceptionHandler.java`)
- ✅ Functional React components with hooks — no class components
- ✅ `useQuery` / `useMutation` from react-query used consistently
- ✅ Zod + react-hook-form used for validation
- ✅ `react-bootstrap` used for most UI components
- ✅ SLF4J logging present in backend
- ✅ Bean Validation annotations used (`@NotNull`, `@Valid`, etc.)
- ✅ `const` used by default in frontend TypeScript
- ✅ PascalCase for React components, camelCase for functions/hooks
- ✅ Integration tests use `@SpringBootTest` and Testcontainers
- ✅ Unit tests use JUnit 5 and Mockito