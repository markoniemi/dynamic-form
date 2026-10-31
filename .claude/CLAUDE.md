# Dynamic Form

Spring Boot + React monolith: the SPA is built into the backend jar and served from `classpath:/static/`. Postgres in prod, H2 in tests, OAuth 2.0 via the sibling `../../oauth2-server` project.

## Modules (each has its own CLAUDE.md with commands and pitfalls)
- `api/` — shared DTOs, published as a library
- `backend/` — Spring Boot REST API and resource server
- `frontend/` — React/TypeScript SPA
- `testcontainers/` — published Testcontainers library for the app image
- `terraform/` (AWS), `debt-bot/` (Python tooling), `docs/` (specs; start with TechnicalSpecification.md and RequirementsSpecification.md)

Conventions live in the skills under `.claude/skills/` (`dynamic-form-backend`, `-frontend`, `-fullstack`, `-review`).

## Repo-wide rules
- Errors are RFC 7807 `ProblemDetail`; validation errors add an `errors` array of `{ field, message, code }` that the frontend maps onto react-hook-form fields
- Changes depending on the auth server (JWT claims/roles, issuer/JWKS, client registrations in `TestcontainersConfig` and docker-compose, demo users) must be verified against `../../oauth2-server`
- Prefer IntelliJ MCP tools (`mcp__idea__*`) over generic file, search, build and git tools

## Workflow
- Do not commit unless asked; plan all changes upfront; use feature branches for significant work
- Commit messages: one line, imperative, capitalized, no period, semicolons between concerns, what and why: `Add i18n for validation messages; replace 26 hardcoded strings in EditForm with translation keys`
