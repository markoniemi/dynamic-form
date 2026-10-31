# frontend

React 19 + TypeScript + Vite 8 SPA. Maven (`frontend-maven-plugin`, Node v24.13.0) builds it and the `backend` module serves it from `classpath:/static/`.

## Commands (from this directory)
- Dev server: `npm run dev` (port 5173; proxies `/api` and `/login/oauth2` to :8080, `/oauth2` to :9000)
- Type check: `npm run compile`; lint: `npm run lint` (`lint:fix` to fix)
- Tests: `npm test` (Vitest run with coverage, jsdom); one file: `npx vitest run test/pages/EditForm.test.tsx`
- Full CI sequence: `npm run ci` (install, compile, build, test)

## Layout
- `src/pages/` routed screens; `src/components/` form fields (`DynamicForm`, `*Field`, `FieldEditor`) and layout
- `src/services/` API clients on top of `http.ts` (`/api` base, bearer token option, throws `ApiValidationError` from `ProblemDetail.errors`)
- `src/types/` shared types; `oidcConfig.ts` OIDC setup via `react-oidc-context`; `configClient.ts` fetches the issuer from `/api/config`
- `src/i18n.ts` i18next; user-visible strings go through translation keys
- Tests live in `test/` (not next to sources): `renderWithProviders.tsx` wraps components, `testdata/` holds fixtures, `setup.ts` is the Vitest setup

## Pitfalls
- Backend validation errors (`{ field, message, code }`) are wired into react-hook-form field errors; keep that mapping when changing forms or `http.ts`
- Test files must match `test/**/*.{test,spec}.*` (see `vitest.config.ts`) or they are not picked up
- `dist/` and `node_modules/` are generated and read-denied; do not edit
