---
name: dynamic-form-fullstack
description: Full-stack feature implementation for the Dynamic Form app. Use when a feature spans both the Spring Boot backend and the React frontend — API contract design, DTO/TypeScript type alignment, and end-to-end implementation.
---

# Dynamic Form — Full Stack Developer

## Workflow: implement backend first, then frontend

1. **Design the API contract** — define the endpoint, HTTP method, request/response DTOs, and error cases before writing any code.
2. **Implement the backend** — entity changes → repository → service → controller → `@RestControllerAdvice` mapping → backend unit tests.
3. **Align the TypeScript types** — update `frontend/src/types/Form.ts` to match the new/changed DTOs exactly.
4. **Implement the frontend** — service method in `formClient.ts` or `formDataClient.ts` → React Query query/mutation → page/component → i18n keys → frontend tests.
5. **Verify end-to-end** — run `mvn test` (backend) and `npm test` (frontend); check the full flow manually if the dev server is running.

---

## API contract design

Before writing code, agree on:

- **URL**: follows `/api/<resource>` pattern (e.g. `/api/forms`, `/api/form-data/{id}`)
- **Method**: `GET` (read), `POST` (create), `PUT` (full update), `DELETE`
- **Request DTO**: annotated with Jakarta Validation (`@NotNull`, `@Size`, `@Valid`)
- **Response DTO**: never exposes JPA entities directly — always a dedicated DTO
- **Error cases**: which standard Java exceptions the service throws, and what HTTP status they map to in `GlobalExceptionHandler`:
  - `NoSuchElementException` → 404
  - `IllegalArgumentException` → 400
  - `IllegalStateException` → 409

---

## DTO ↔ TypeScript type alignment

Every backend DTO must have a matching TypeScript interface in `frontend/src/types/Form.ts`.

| Backend (Java) | Frontend (TypeScript) |
|---|---|
| `String` | `string` |
| `Long` / `Integer` | `number` |
| `Boolean` | `boolean` |
| `LocalDateTime` | `string` (ISO 8601) |
| `List<T>` | `T[]` |
| `Map<String, Object>` | `Record<string, unknown>` |
| nullable field | `field?: Type` or `field: Type \| null` |

When changing a DTO, always update the TypeScript type in the same change.

---

## Key files to touch for a full-stack feature

### Backend
| File | Purpose |
|---|---|
| `entity/` | JPA entity — add fields here first |
| `dto/` | Request and response DTOs |
| `mapper/` | MapStruct interface — add new mappings |
| `repository/` | Spring Data JPA — add query methods if needed |
| `service/` | Business logic — throws standard Java exceptions |
| `controller/` | REST endpoint — delegates to service, uses `@PreAuthorize` |
| `config/GlobalExceptionHandler.java` | Maps exceptions to HTTP status codes |

### Frontend
| File | Purpose |
|---|---|
| `src/types/Form.ts` | TypeScript interfaces — mirror backend DTOs |
| `src/services/http.ts` | Low-level fetch wrapper — do not modify for features |
| `src/services/formClient.ts` | Form-related API calls |
| `src/services/formDataClient.ts` | Submission-related API calls |
| `src/pages/` | Route-level components — use React Query here |
| `src/components/` | Reusable UI components |
| `public/locales/en/translation.json` | i18n strings — add keys for new UI text |

---

## Adding a new API endpoint — checklist

### Backend
- [ ] DTO created in `dto/` with Jakarta Validation annotations
- [ ] MapStruct mapper updated in `mapper/`
- [ ] Service method added; throws only standard Java exceptions
- [ ] Controller method added with `@PreAuthorize`
- [ ] Exception → HTTP mapping verified in `GlobalExceptionHandler`
- [ ] Unit test added for the service method
- [ ] Controller test added (mock service, verify status + response body)

### Frontend
- [ ] TypeScript interface in `Form.ts` matches response DTO exactly
- [ ] Service method added to `formClient.ts` or `formDataClient.ts` using `http.request`
- [ ] `useQuery` or `useMutation` wired in the page/component
- [ ] `enabled` guard set when required params may be absent (use `?? ''` fallback in `queryFn`)
- [ ] Mutation invalidates relevant `queryKey` values on success
- [ ] Loading and error states handled in the UI
- [ ] New user-visible strings added to i18n (`t('key.path')`)
- [ ] Component test covers the happy path and at least one error/loading state

---

## Auth pattern

The token flows from OIDC context → service call → `Authorization` header:

```typescript
const { user } = useAuth();
const token = user?.access_token;

// In queryFn — enabled guard prevents execution when token is absent:
useQuery({
  queryKey: ['resource', id],
  queryFn: () => formClient.getResource(id ?? '', token ?? ''),
  enabled: !!id && !!token,
});

// In mutationFn — explicit rejection when token is missing:
mutationFn: (data) => {
  if (!token) return Promise.reject(new Error('Not authenticated'));
  return formClient.saveResource(data, token);
},
```

On the backend, endpoints are secured with `@PreAuthorize("isAuthenticated()")` or role-based expressions. The JWT is validated by the OAuth2 Resource Server configuration — no manual token parsing in controllers or services.
