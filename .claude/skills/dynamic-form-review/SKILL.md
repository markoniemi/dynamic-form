---
name: dynamic-form-review
description: "Review Java or TypeScript/React code in this project against the project's coding standards (Google Java Style, Spring patterns, React 19 + TypeScript conventions). Invoke with a file path or paste code to review."
---

# Dynamic Form Code Review

Review the specified file against this project's standards. If no file specified, ask user which file to review.

## How to review

1. Read target file(s).
2. Check relevant checklist below.
3. Report findings grouped by severity:
   - **Must fix** — security, correctness, or hard project rules
   - **Should fix** — style or maintainability issues
   - **Consider** — optional improvements
4. For each finding: file path + line number, rule violated, one-line fix.
5. Brief summary paragraph.

Do NOT rewrite whole file. Propose targeted edits only.

---

## Java / Spring Boot checklist

### Style & Formatting
- [ ] Two-space indentation; clear names; no commented code; Javadoc only for public API

### Architecture
- [ ] Controllers delegate only; services contain business logic
- [ ] DTOs for all API input/output; MapStruct mapper for each entity↔DTO conversion
- [ ] Constructor injection preferred; no field injection with `@Autowired` (`@Value` on constructor params OK)
- [ ] Config via `@Value` or `@ConfigurationProperties`, not hardcoded strings
- [ ] DTOs immutable (`@Value`)

### Exceptions & Error Handling
- [ ] Services throw standard Java exceptions only (`NoSuchElementException`, `IllegalArgumentException`, `IllegalStateException`)
- [ ] No `ResponseStatusException` in services
- [ ] HTTP mapping centralized in `@RestControllerAdvice`

### Security & Validation
- [ ] Endpoints secured with `@PreAuthorize`
- [ ] Input validated with Jakarta Validation (`@NotNull`, `@Size`, etc.)
- [ ] No sensitive data logged

### Testing
- [ ] JUnit 5 + Mockito for unit tests
- [ ] `@SpringBootTest` + Testcontainers (PostgreSQL) or `@DataJpaTest` (H2) for integration
- [ ] Test names: `methodNameWithStateUnderTestExpectedBehavior` (e.g., `getFormByIdWithUnknownIdThrowsException`)
- [ ] JUnit 5 assertions; no Spring-specific exceptions in assertions

### Single Responsibility
- [ ] One reason to change per class/method
- [ ] Methods ≤ ~20 lines; no DRY violations

### Utility Libraries
- [ ] Use Apache Commons for null/empty checks instead of manual conditionals

### Dependencies
- [ ] Check if dependencies have newer versions or are unmaintained (`mvn dependency:tree`, Maven Central, GitHub activity)

---

## TypeScript / React checklist

### Types & Components
- [ ] No `any`; all props typed; API shapes synced with backend DTOs
- [ ] Functional components; `const` by default; filenames match exports

### Hooks & Data Fetching
- [ ] Hooks use `use` prefix; not called conditionally
- [ ] `useQuery` / `useMutation` from `@tanstack/react-query` for server state
- [ ] No direct `fetch` in components; all HTTP via `formClient` or service module
- [ ] Query keys stable; queries guarded with `enabled` when needed

### Auth & Routing
- [ ] Auth via `useAuth` from `react-oidc-context`
- [ ] Token from `user?.access_token` → `formClient`
- [ ] Routes in `src/components/Content.tsx`

### Forms & Validation
- [ ] `react-hook-form` for user forms
- [ ] Zod for validation schemas
- [ ] Reuse `DynamicForm` and field components
- [ ] Types aligned with backend DTOs

### Internationalization
- [ ] All user-visible strings via `useTranslation` + `t('key.path')`
- [ ] No hardcoded strings in JSX
- [ ] Translation keys are literals (no dynamic `` t(`section.${var}`) ``)

### Style & Accessibility
- [ ] `react-bootstrap` + Bootstrap utilities preferred
- [ ] Semantic HTML; ARIA where needed
- [ ] No unnecessary dependencies

### Testing
- [ ] Vitest + `@testing-library/react`
- [ ] `userEvent` over `fireEvent`
- [ ] Assert on user sees/can do; not implementation details

### Dependencies
- [ ] Check if dependencies have newer versions or are unmaintained (`npm outdated`, npm registry, GitHub activity)