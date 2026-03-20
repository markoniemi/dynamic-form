# Code Review Findings

Generated: 2026-03-20. Updated: 2026-03-20 (re-reviewed against Clean Code + Effective Java rules).
Based on project standards defined in `.github/copilot-instructions.md` and `skills/dynamic-form-backend/SKILL.md`.

Legend: `[ ]` open · `[x]` fixed · `[-]` won't fix · `⬆` severity upgraded in re-review · `🆕` new finding

---

## Backend (Java)

### Must Fix

- [ ] **B1** — `dto/FormDataDto.java:11-13`
  Both `@Data` and `@Value` used together — contradictory annotations. `@Data` generates mutable setters while `@Value` enforces immutability.
  **Fix:** Remove `@Data`, keep `@Value` (adds `@AllArgsConstructor` + makes fields final).

- [ ] **B2** — `service/FormDataService.java:46,76`
  Service throws `AccessDeniedException` (a Spring Security exception). Services must only throw standard Java exceptions (Effective Java Item 72).
  **Fix:** Replace with `IllegalArgumentException` or `IllegalStateException`; let `GlobalExceptionHandler` map it to 403.

- [ ] **B3** — `controller/FormDataController.java:78`
  `AccessDeniedException` thrown directly from a controller method instead of being handled centrally.
  **Fix:** Move the authorization check to a `@PreAuthorize` annotation or throw a standard exception from the service and map it in `GlobalExceptionHandler`.

- [ ] **B4** — `util/SecurityUtils.java`
  File uses 4-space indentation. Project requires 2-space (Google Java Style).
  **Fix:** Reformat to 2-space indentation throughout the file.

- [ ] **B8** ⬆ — `config/WebConfig.java:16`
  WebJar path contains hardcoded version `1.0.0-SNAPSHOT`. Hardcoded config values violate the externalization rule (Effective Java Item 5 spirit — don't hardwire resources).
  **Fix:** Externalize to `application.yaml` and inject with `@Value("${frontend.webjar.version}")`.

- [ ] **B9** ⬆ — `config/SecurityConfig.java:47-48`
  CORS allowed origins (`http://localhost:8080`, `http://localhost:5173`, `http://localhost:9000`) are hardcoded in source — must differ per environment.
  **Fix:** Move to `application.yaml` as `cors.allowed-origins` list; inject with `@ConfigurationProperties`.

### Should Fix

- [ ] **B5** — `service/FormServiceTest.java:25,49,60`
  Test method names (`getForm()`, `saveForm()`, `existsByFormKey()`) don't follow the project naming convention.
  **Fix:** Rename to descriptive forms, e.g. `getFormWithValidKeyReturnsForm()`, `saveFormPersistsFormData()`, `existsByFormKeyReturnsTrueForExistingKey()`.

- [ ] **B6** — `controller/FormDataControllerTest.java:106`
  Delete test uses hardcoded `"user"` string that may not match the JWT principal configured elsewhere in the test setup.
  **Fix:** Use the same username value as the JWT mock in the test base class.

- [ ] **B7** — `controller/GlobalExceptionHandler.java:46-50`
  Handler catches `SecurityException` but the service throws `AccessDeniedException` — the handler misses the actual exception being thrown.
  **Fix:** After fixing B2/B3, add or replace handler to cover the correct exception type.

- [ ] **B10** — `e2e/pages/BasePage.java:20-26`
  Uses `Thread.sleep()` for waits in Selenium tests, and swallows `InterruptedException` silently (Effective Java Item 77 — never swallow exceptions).
  **Fix:** Replace with `WebDriverWait` + `ExpectedConditions`; if sleep must stay, call `Thread.currentThread().interrupt()` in the catch block.

- [ ] **B11** — `service/FormDataService.java:41`
  `@NotNull` on service method parameters is redundant — Jakarta Validation only runs via the bean validation framework on controller inputs.
  **Fix:** Remove `@NotNull` from service method signatures; use `Objects.requireNonNull()` if null-guarding is genuinely needed (Effective Java Item 49).

### Consider

- [ ] **B12** 🆕 — `controller/FormController.java`, `controller/FormDataController.java`
  Controllers don't document which exceptions propagate from the service layer.
  **Fix:** Add Javadoc `@throws` clauses on controller methods documenting expected service exceptions.

- [ ] **B13** 🆕 — `mapper/FormListItemMapper.java`
  Single-method mapper interface is separate from `FormMapper` with no clear boundary reason.
  **Fix:** Consider consolidating into `FormMapper` if the mapping is related, or leave as-is if the separation is intentional.

---

## Frontend (TypeScript / React)

### Must Fix — i18n

- [ ] **F1** — `components/Navigation.tsx:37`
  `"Create Form"` is a hardcoded string.
  **Fix:** `{t('navigation.createForm')}` and add key to translation files.

- [ ] **F2** — `components/Navigation.tsx:46`
  `"Language"` is a hardcoded string.
  **Fix:** `{t('navigation.language')}`.

- [ ] **F3** — `components/Navigation.tsx:47`
  `"English"` is a hardcoded string.
  **Fix:** `{t('navigation.english')}`.

- [ ] **F4** — `pages/EditForm.tsx:146`
  `"← Back to Forms"` is a hardcoded string.
  **Fix:** `{t('editForm.backToForms')}`.

- [ ] **F5** — `pages/EditForm.tsx:153`
  `"Create New Form"` is a hardcoded string.
  **Fix:** `{t('editForm.title')}`.

- [ ] **F6** — `pages/EditForm.tsx:198`
  `"Fields"` is a hardcoded string.
  **Fix:** `{t('editForm.fields')}`.

- [ ] **F7** — `pages/EditForm.tsx:214`
  `"+ Add Field"` is a hardcoded string.
  **Fix:** `{t('editForm.addField')}`.

- [ ] **F8** — `pages/EditForm.tsx:224`
  `"Creating..."` is a hardcoded string.
  **Fix:** `{t('editForm.creating')}`.

- [ ] **F9** — `pages/EditForm.tsx:227`
  `"Create Form"` is a hardcoded string.
  **Fix:** `{t('editForm.submit')}`.

- [ ] **F10** — `pages/EditForm.tsx:231`
  `"Cancel"` is a hardcoded string.
  **Fix:** `{t('common.cancel')}` (or existing common key if one exists).

- [ ] **F18** 🆕 — `components/Content.tsx:22,35,38,75,77`
  `"Loading..."`, `"Authentication Error"`, `"Try Again"`, `"Welcome"`, `"Please log in."`, `"Login with OAuth2"` are all hardcoded.
  **Fix:** Replace each with `t('content.<key>')` and add keys to translation files.

- [ ] **F19** 🆕 — `components/ReadOnlyDynamicForm.tsx:17,21`
  `"—"`, `"Yes"`, and `"No"` are hardcoded display strings.
  **Fix:** `{t('readOnlyForm.empty')}`, `{t('readOnlyForm.yes')}`, `{t('readOnlyForm.no')}`.

- [ ] **F20** 🆕 — `components/SelectField.tsx:19`
  `"Select an option..."` placeholder is hardcoded.
  **Fix:** `{t('common.selectOption')}`.

- [ ] **F24** 🆕 — `pages/EditForm.tsx:164`
  `"e.g., contact-form"` placeholder is hardcoded.
  **Fix:** `{t('editForm.formKeyPlaceholder')}`.

- [ ] **F25** 🆕 — `pages/EditForm.tsx:178`
  `"e.g., Contact Form"` placeholder is hardcoded.
  **Fix:** `{t('editForm.titlePlaceholder')}`.

- [ ] **F26** 🆕 — `pages/EditForm.tsx:191`
  `"Brief description of the form"` placeholder is hardcoded.
  **Fix:** `{t('editForm.descriptionPlaceholder')}`.

### Must Fix — Type Safety

- [ ] **F11** — `services/http.ts:45`
  `null as unknown as T` cast bypasses type safety for non-JSON responses.
  **Fix:** Change the return type to `T | null` and let callers handle the null case explicitly.

- [ ] **F12** — `components/DynamicForm.tsx:22`
  `errors[field.name]?.message as string | undefined` — unsafe cast; `message` could be a non-string `FieldError` property.
  **Fix:** Add a type guard: `typeof msg === 'string' ? msg : undefined`.

### Should Fix — Testing

- [ ] **F13** — `test/pages/FormSubmission.test.tsx:122,125,128,168,189`
  Uses `fireEvent.change()` / `fireEvent.click()` instead of `userEvent`.
  **Fix:** Replace with `await userEvent.type()` / `await userEvent.click()` (import from `@testing-library/user-event`).

- [ ] **F14** — `test/pages/FormSubmissions.test.tsx:148,159,170`
  Uses `fireEvent.click()`.
  **Fix:** Replace with `await userEvent.click()`.

- [ ] **F15** — `test/pages/SubmissionDetail.test.tsx:162`
  Uses `fireEvent.click()`.
  **Fix:** Replace with `await userEvent.click()`.

- [ ] **F16** — `test/components/DynamicForm.test.tsx:12`
  `as unknown as UseFormRegister<FormValues>` double-cast in test mock.
  **Fix:** Use `vi.fn() as unknown as UseFormRegister<FormValues>` or define a properly typed stub.

### Should Fix — Safety & Style

- [ ] **F21** 🆕 — `pages/EditForm.tsx:44`
  `token!` non-null assertion used before calling `formClient.saveForm()` without an earlier guard.
  **Fix:** Add `if (!token) { setError('Not authenticated'); return; }` before the mutation call.

- [ ] **F22** 🆕 — `components/FieldEditor.tsx:5-8`
  `FieldTypeOption` interface is defined inside the component file instead of in `src/types/`.
  **Fix:** Move to `frontend/src/types/Form.ts` and import it in `FieldEditor.tsx`.

### Consider

- [ ] **F17** — `pages/EditForm.tsx:10-20`
  `FIELD_TYPES` constant defined locally in the page component.
  **Fix:** Move to `types/Form.ts` or a dedicated `constants/` file so it can be shared.

- [ ] **F23** 🆕 — `pages/SubmissionDetail.tsx:78`
  Back link uses a raw `←` arrow character with no ARIA label, which screen readers may announce awkwardly.
  **Fix:** Add `aria-label="Back"` to the link element, or use a visually hidden span for the arrow.
