# Code Review Findings

Generated: 2026-03-20. Updated: 2026-04-04 (re-reviewed frontend against authoritative TypeScript sources).
Based on project standards defined in `.github/copilot-instructions.md` and `skills/dynamic-form-review/SKILL.md`.

Legend: `[ ]` open · `[x]` fixed · `[-]` won't fix · `⬆` severity upgraded in re-review · `🆕` new finding

---

## Backend (Java)

### Must Fix

- [x] **B1** — `dto/FormDataDto.java:11-13`
  Both `@Data` and `@Value` used together — contradictory annotations. `@Data` generates mutable setters while `@Value` enforces immutability.
  **Fix:** Remove `@Data`, keep `@Value` (adds `@AllArgsConstructor` + makes fields final).

- [x] **B2** — `service/FormDataService.java:46,76`
  Service throws `AccessDeniedException` (a Spring Security exception). Services must only throw standard Java exceptions (Effective Java Item 72).
  **Fix:** Replace with `java.lang.SecurityException`; `GlobalExceptionHandler` already maps it to 403. Also fixes B7.

- [x] **B3** — `controller/FormDataController.java:78`
  `AccessDeniedException` thrown directly from a controller method instead of being handled centrally.
  **Fix:** Move the authorization check to a `@PreAuthorize` annotation or throw `SecurityException` from the service and let `GlobalExceptionHandler` map it to 403.

- [x] **B4** — `util/SecurityUtils.java`
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
  **Fix:** Resolved automatically when B2/B3 are fixed by switching to `SecurityException`; no separate handler change needed.

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

- [x] **F1** — `components/Navigation.tsx:37`
  `"Create Form"` is a hardcoded user-visible string.
  **Fix:** `{t('navigation.createForm')}` and add key to translation files.

- [x] **F2** — `components/Navigation.tsx:46`
  `"Language"` is a hardcoded user-visible string.
  **Fix:** `{t('navigation.language')}`.

- [x] **F3** — `components/Navigation.tsx:47`
  `"English"` is a hardcoded user-visible string.
  **Fix:** `{t('navigation.english')}`.

- [x] **F4** — `pages/EditForm.tsx:146`
  `"← Back to Forms"` is a hardcoded user-visible string.
  **Fix:** `{t('editForm.backToForms')}`.

- [x] **F5** — `pages/EditForm.tsx:153`
  `"Create New Form"` is a hardcoded user-visible string.
  **Fix:** `{t('editForm.title')}`.

- [x] **F6** — `pages/EditForm.tsx:198`
  `"Fields"` is a hardcoded user-visible string.
  **Fix:** `{t('editForm.fields')}`.

- [x] **F7** — `pages/EditForm.tsx:214`
  `"+ Add Field"` is a hardcoded user-visible string.
  **Fix:** `{t('editForm.addField')}`.

- [x] **F8** — `pages/EditForm.tsx:224`
  `"Creating..."` is a hardcoded user-visible string.
  **Fix:** `{t('editForm.creating')}`.

- [x] **F9** — `pages/EditForm.tsx:227`
  `"Create Form"` is a hardcoded user-visible string.
  **Fix:** `{t('editForm.submit')}`.

- [x] **F10** — `pages/EditForm.tsx:231`
  `"Cancel"` is a hardcoded user-visible string.
  **Fix:** `{t('common.cancel')}` (or existing common key if one exists).

- [x] **F18** 🆕 — `components/Content.tsx:22,35,38,75,77`
  `"Loading..."`, `"Authentication Error"`, `"Try Again"`, `"Welcome"`, `"Please log in."`, `"Login with OAuth2"` are all hardcoded user-visible strings.
  **Fix:** Replace each with `t('content.<key>')` and add keys to translation files.

- [x] **F19** 🆕 — `components/ReadOnlyDynamicForm.tsx:17,21`
  `"—"`, `"Yes"`, and `"No"` are hardcoded user-visible strings.
  **Fix:** `{t('common.empty')}`, `{t('common.yes')}`, `{t('common.no')}` — these are generic values reusable across components.

- [x] **F20** 🆕 — `components/SelectField.tsx:19`
  `"Select an option..."` placeholder is a hardcoded user-visible string.
  **Fix:** Used existing `{t('form.selectOption')}` (key already existed; no duplicate added).

- [x] **F24** 🆕 — `pages/EditForm.tsx:164`
  `"e.g., contact-form"` placeholder is a hardcoded user-visible string.
  **Fix:** `{t('editForm.formKeyPlaceholder')}`.

- [x] **F25** 🆕 — `pages/EditForm.tsx:178`
  `"e.g., Contact Form"` placeholder is a hardcoded user-visible string.
  **Fix:** `{t('editForm.titlePlaceholder')}`.

- [x] **F26** 🆕 — `pages/EditForm.tsx:191`
  `"Brief description of the form"` placeholder is a hardcoded user-visible string.
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

### Should Fix — Safety & Style

- [ ] **F21** 🆕 — `pages/EditForm.tsx:44`
  `token!` non-null assertion used before calling `formClient.saveForm()` without an earlier guard.
  **Fix:** Add `if (!token) { setError('Not authenticated'); return; }` before the mutation call.

### Consider

- [ ] **F17** — `pages/EditForm.tsx:10-20`
  `FIELD_TYPES` constant defined locally in the page component.
  **Fix:** Move to `types/Form.ts` or a dedicated `constants/` file so it can be shared.

- [ ] **F16** — `test/components/DynamicForm.test.tsx:12`
  `as unknown as UseFormRegister<FormValues>` cast in test mock with no explanation. The `as unknown as T` pattern is permitted by the skill at well-understood boundaries, but must be documented.
  **Fix:** Add an inline comment explaining why the cast is safe (e.g. `// mock; only subset of register API exercised in this test`).

- [ ] **F22** 🆕 — `components/FieldEditor.tsx:5-8`
  `FieldTypeOption` interface is defined inside the component file. The skill permits component-specific types to live near the component; only move if the type is reused elsewhere.
  **Fix:** If used in more than one component, move to `frontend/src/types/Form.ts` and import it in `FieldEditor.tsx`.

- [ ] **F23** 🆕 — `pages/SubmissionDetail.tsx:78`
  Back link uses a raw `←` arrow character with no ARIA label, which screen readers may announce awkwardly.
  **Fix:** Add `aria-label="Back"` to the link element, or use a visually hidden span for the arrow.

---

## Frontend — Authoritative TypeScript Sources Review (2026-04-04)

Sources: Effective TypeScript 2nd Ed, Programming TypeScript (O'Reilly), Google TypeScript Style Guide (gts), TypeScript Do's and Don'ts, @typescript-eslint strict-type-checked, Microsoft TypeScript Coding Guidelines.

### Must Fix — Type Safety

- [ ] **F27** 🆕 — `components/DynamicForm.tsx:49`
  Non-exhaustive switch: `default: return null` silently drops unknown `FormField.type` values. If a new type is added to the union, the compiler will not flag the missing branch.
  **Source:** Effective TypeScript Item 36; @typescript-eslint `switch-exhaustiveness-check`.
  **Fix:** Replace `default: return null` with an exhaustive check:
  ```ts
  default: {
    const _exhaustive: never = field.type;
    return null;
  }
  ```

- [ ] **F28** 🆕 — `pages/FormSubmission.tsx:28`
  `useForm()` called without a type parameter. Defaults to `FieldValues` which is `Record<string, any>`, infecting `onSubmit(data)` at line 86 and `setValue(key, value)` at line 53 with `any`.
  **Source:** Effective TypeScript Item 5 ("avoid `any` types"); @typescript-eslint `no-unsafe-argument`.
  **Fix:** Supply the generic: `useForm<FormValues>()` (import `FormValues` from `types/Form.ts`).

- [ ] **F29** 🆕 — `FormSubmission.tsx:110,136` · `FormSubmissions.tsx:36` · `Forms.tsx:35` · `SubmissionDetail.tsx:54`
  `(error as Error).message` — type assertion used instead of type narrowing. While react-query types `error` as `Error | null` and the surrounding `if` narrows out `null`, the `as Error` cast is still a code smell that bypasses the type system unnecessarily.
  **Source:** TypeScript Do's and Don'ts ("don't use type assertions when a type guard is possible"); Google TS Style Guide.
  **Fix:** Remove the cast — after the `if (error)` guard, TypeScript already narrows to `Error`: use `error.message` directly. If the type is `unknown`, narrow with `error instanceof Error ? error.message : String(error)`.

### Must Fix — i18n (missed in original review)

- [ ] **F30** 🆕 — `pages/EditForm.tsx:84,88,92,96,103,107,111,117,122`
  8 `setError()` calls use hardcoded English strings (`'Form key is required'`, `'Title is required'`, etc.) that are displayed in the UI via the `<Alert>` at line 159.
  **Fix:** Replace with `setError(t('editForm.validation.formKeyRequired'))` etc. and add keys to translation files.

- [ ] **F31** 🆕 — `pages/EditForm.tsx:165,173,179,191`
  Form labels `"Form Key"`, `"Title"`, `"Description"` and help text `"Unique identifier (lowercase letters, numbers, hyphens only)"` are hardcoded English strings.
  **Fix:** Replace with `t('editForm.label.formKey')`, `t('editForm.label.title')`, `t('editForm.label.description')`, `t('editForm.help.formKey')`.

- [ ] **F32** 🆕 — `components/FieldEditor.tsx:68,103,106,113,125,143,148,155,165,196`
  10+ hardcoded user-visible strings: `"Field {n}"`, `"Field Name"`, `"Label"`, `"Type"`, `"Placeholder"`, `"Optional placeholder text"`, `"Required"`, `"Options"`, `"Value"`, `"Label"` (option), `"+ Add Option"`, and button titles `"Move up"`, `"Move down"`, `"Remove field"`.
  **Fix:** Replace each with `t('fieldEditor.<key>')` and add keys to translation files.

### Should Fix — Tooling

- [ ] **F33** 🆕 — `eslint.config.js:11`
  Uses `tseslint.configs.strict` but not `strictTypeChecked`. Without type-aware linting, floating promises (`no-floating-promises`), `any` infection (`no-unsafe-*`), and unsafe returns go undetected.
  **Source:** @typescript-eslint strict-type-checked documentation.
  **Fix:** Replace `...tseslint.configs.strict` with `...tseslint.configs.strictTypeChecked` and add `languageOptions.parserOptions.project: true` to enable type information.

### Should Fix — Type Safety

- [ ] **F34** 🆕 — `types/Form.ts:1-43`
  All interfaces (`FormField`, `FieldOption`, `Form`, `FormDataDto`, `FormListItem`) use mutable properties. Types representing API responses should be `readonly` by default to prevent accidental mutation.
  **Source:** Programming TypeScript (readonly-by-default); Effective TypeScript Item 17.
  **Fix:** Add `readonly` to all property definitions on API response types. For types also used in form editing (e.g. `FormField` in `EditForm.tsx`), consider splitting into `ReadonlyFormField` (for display) and mutable `EditableFormField` (for editing), or use `Readonly<FormField>` at consumption sites.

- [ ] **F35** 🆕 — `i18n.ts:15-30`
  `format` callback returns raw `value` at line 30 without narrowing. The i18next callback expects `string` returns, but when `value` is not a `Date`, the function returns whatever was passed in (typed `any` by i18next).
  **Source:** TypeScript Do's and Don'ts; Effective TypeScript Item 5.
  **Fix:** Replace `return value;` with `return String(value);` to guarantee a string return.

### Consider

- [ ] **F36** 🆕 — `pages/EditForm.tsx:11-21`
  `FIELD_TYPES` uses `as const` but doesn't validate that `value` properties match `FormField['type']`. If the union changes, the constant won't cause a compile error.
  **Source:** Effective TypeScript 2nd Ed (`satisfies` operator).
  **Fix:** Add type validation:
  ```ts
  const FIELD_TYPES = [
    {value: 'text', label: 'Text'},
    // ...
  ] as const satisfies ReadonlyArray<{readonly value: FormField['type']; readonly label: string}>;
  ```

- [ ] **F37** 🆕 — `context/oidcConfig.tsx:1-9`
  `oidcConfig` is an untyped object literal. No compile-time validation that its shape matches what `AuthProvider` expects.
  **Source:** Effective TypeScript 2nd Ed (`satisfies` operator).
  **Fix:** `const oidcConfig = { ... } satisfies AuthProviderProps;` (import from `react-oidc-context`).

- [ ] **F38** 🆕 — `tsconfig.app.json`
  `exactOptionalPropertyTypes` is not enabled. Without it, TypeScript allows assigning `undefined` to optional properties (e.g. `placeholder: undefined` on `FormField`) even when the intent is "property not present".
  **Source:** TypeScript Performance Wiki; Microsoft TypeScript Coding Guidelines.
  **Fix:** Add `"exactOptionalPropertyTypes": true` to `compilerOptions`. Requires fixing any existing `prop: undefined` assignments.
