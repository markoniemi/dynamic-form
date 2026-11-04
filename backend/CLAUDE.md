# backend

Spring Boot 4.0.3 / Java 21 REST API and OAuth2 resource server. Packages under `com.example.backend`. Depends on `api` (DTOs) and `frontend` (built SPA, served as static resources).

## Commands (from repo root)
- Unit tests: `mvn -pl backend test`
- Unit + integration tests: `mvn -pl backend verify` (ITs run via failsafe; Docker must be running)
- Run locally: start `DevelopmentApplication` (test sources) or `mvn -pl backend spring-boot:run -Dspring-boot.run.profiles=dev`; `dev` needs Postgres on `localhost:5432/template` and the auth server on `localhost:9000`

## Layout
- `controller/` (+ `GlobalExceptionHandler`) → `service/` → `repository/`; `entity/` JPA, `mapper/` MapStruct, DTOs live in the `api` module
- `config/SecurityConfig`: `/api/**` authenticated, `/api/config/**` public, stateless JWT; roles come from `RoleClaimsConverter`; admin-only endpoints use `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`
- `config/DatabaseInitializer` seeds `src/main/resources/forms/*.json` on startup, skipping existing `formKey`s
- Profiles: `dev` (Postgres, `ddl-auto: update`), `prod` (env vars `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `OAUTH2_ISSUER_URI`; Flyway on, `ddl-auto: validate`), `test` (H2, `create-drop`)

## Tests
- Unit/slice tests are `*Test`; integration tests are `*IT` and extend `IntegrationTestBase` (`@SpringBootTest` on port 8080, profile `test`)
- `TestcontainersConfig` starts the auth server from `src/test/resources/oauth2-server.yaml`, bound to host port 9000
- `e2e/` ITs drive the real UI with Playwright (`PlaywrightConfig`, page objects in `e2e/pages/`)

## Pitfalls
- Flyway is disabled outside `prod`: schema changes need a new `db/migration/V<n>__*.sql`, never edit an applied one; dev/test schemas come from Hibernate
- Port 9000 is fixed: the token `iss` must equal the backend's issuer. The shutdown hook in `TestcontainersConfig` frees it for the `testcontainers` module ITs, so keep it
- `jib` builds the Docker image in `pre-integration-test` (skipped with `-DskipTests`); the `ci` profile pushes it to ghcr.io on `deploy`
- Validation errors must come back as RFC 7807 `ProblemDetail` with an `errors` array; the frontend maps it onto form fields
