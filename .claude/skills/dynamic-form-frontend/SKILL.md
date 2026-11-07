---
name: dynamic-form-frontend
description: "Work on the dynamic-form React/Vite frontend (React 19 + TypeScript). Use when editing frontend UI, routing, auth/OIDC, react-query data fetching, react-hook-form + zod validation, i18n, or Bootstrap-based components under frontend/."
---

# Dynamic Form Frontend

## Scope and defaults

- Work inside frontend/ for UI changes; keep backend untouched unless asked.
- Use React function components and hooks only.
- Use TypeScript types from frontend/src/types first; extend them when needed.
- Prefer react-bootstrap components and Bootstrap utility classes.

## Project map (frontend/)

`frontend/CLAUDE.md` owns the layout — read it rather than restating it here. Two entry points it does not name:

- src/App.tsx: Router + AuthProvider.
- src/main.tsx: QueryClientProvider + app bootstrap.

## Data access and caching

- Use @tanstack/react-query for reads and writes.
- For queries, set stable queryKey arrays; enable only when required params exist.
- For mutations, call formClient methods and invalidate related queryKey values on success.
- Do not call fetch directly in pages; extend formClient or add a new service wrapper that uses http.request.

## Auth and routing

- Read auth state with useAuth from react-oidc-context.
- Pull the token from user?.access_token and pass it to formClient.
- Keep route definitions in src/components/Content.tsx.
- Use Navigate for unknown routes and handle unauthenticated flows with signinRedirect.

## Forms and validation

- For user-entered forms, use react-hook-form; pair with zod when adding new validation.
- For dynamic form rendering, reuse DynamicForm + field components (TextField, SelectField, etc.).
- For read-only views, use ReadOnlyDynamicForm.
- Keep FormField and FormValues aligned with backend DTOs; update frontend/src/types/Form.ts when schemas change.

## i18n

- No hardcoded user-visible strings in JSX or component logic — wrap every UI string with `t('key.path')`.
- Translation keys must be hardcoded string literals (e.g. `t('navigation.createForm')`). Dynamic key construction (e.g. `t(\`section.${var}\``) breaks static extraction tools like i18next-parser and typed-i18n plugins.
- Keep date formatting in i18n (see common.date.long usage).

## Testing

- Write component tests with Vitest + @testing-library/react.
- Prefer user-event for interactions.

## When adding a new page

1. Create a component in src/pages.
2. Wire it into Content routes.
3. Add required service calls in formClient or a new service module using http.request.
4. Add i18n keys for new user-facing strings.

## Style reference

General TypeScript and React guidance is in `references/ts-react-style.md`; read it only when a style question actually comes up.

