---
name: dynamic-form-backend
description: Expert knowledge for the Dynamic Form backend (Spring Boot, Java, PostgreSQL). Use this skill when working on backend code, fixing bugs, or adding features to the API.
---

# Dynamic Form Backend

## Overview

This skill provides context and workflows for the `backend` module of the Dynamic Form application. It is a Spring Boot application serving as an OAuth2 Resource Server. For the framework and language versions, see `backend/CLAUDE.md` — they are not repeated here.

## Technology Stack

- **Security:** Spring Security OAuth2 Resource Server
- **Data:** Spring Data JPA, Hibernate, PostgreSQL (prod and dev), H2 (test only)
- **Utilities:** MapStruct (DTO mapping), Lombok (boilerplate reduction)
- **Testing:** JUnit 5, Mockito, Testcontainers, WebTestClient

## Project Structure

`backend/CLAUDE.md` owns the package layout and the profile list — read it rather than restating it here. Three things it does not spell out:

- DTOs are **not** in this module: they live in the `api` module, package `com.example.dynamicform.api`
- `src/main/resources/` holds `application.yaml` plus one file per profile: `application-dev.yaml` (Postgres), `application-prod.yaml` (Postgres), `application-test.yaml` (H2)
- `src/test/java/com/example/backend/IntegrationTestBase.java` is the base class for integration tests (Testcontainers setup)

## Development Workflow

### Running the Application

Run every Maven command from the repository root with `-pl backend`. Never `cd backend` first: Maven can then report `BUILD SUCCESS` without running any tests (see `.claude/commands/test.md`).

To run the application locally with the `dev` profile:

```bash
mvn -pl backend spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile needs Postgres on `localhost:5432/template` and the auth server on `localhost:9000`. Starting `DevelopmentApplication` (in test sources) does the same thing.

The API will be available at `http://localhost:8080/api`.

### Testing

**Unit Tests:**
```bash
mvn -pl backend test
```

**Integration Tests** (requires Docker for Testcontainers):
```bash
mvn -pl backend verify
```

### Database

- **Production:** PostgreSQL, Flyway on, `ddl-auto: validate`
- **Development:** PostgreSQL on `localhost:5432/template`, `ddl-auto: update`
- **Test:** H2 in-memory, `ddl-auto: create-drop`
- **Schema Management:** Flyway is enabled in `prod` only; dev and test schemas come from Hibernate. A schema change needs a new `db/migration/V<n>__*.sql` — never edit an applied one.

## Coding Conventions

- **DTOs:** Always use DTOs for API requests and responses. Never expose Entities directly.
- **Mappers:** Use MapStruct interfaces for converting between Entities and DTOs.
- **Dependency Injection:** Use constructor injection (Lombok `@RequiredArgsConstructor`).
- **Validation:** Use Jakarta Validation annotations (`@NotNull`, `@Size`, etc.) on DTOs.
- **Error Handling:** Throw standard Java exceptions from the service layer — never HTTP-specific exceptions like `ResponseStatusException`. Map all exceptions to HTTP responses once, centrally, in a `@RestControllerAdvice`. `controller/GlobalExceptionHandler` is the source of truth for this mapping; the `dynamic-form-fullstack` skill carries the same list and both must change with it:

  | Exception | HTTP status |
  |---|---|
  | `MethodArgumentNotValidException` | 400, with the `errors` array |
  | `NoSuchElementException` | 404 Not Found |
  | `IllegalArgumentException` | 400 Bad Request |
  | `IllegalStateException` | 409 Conflict |
  | `SecurityException` / `AccessDeniedException` | 403 Forbidden |
  | anything unhandled | 500 Internal Server Error |

General Java and clean-code guidance is in `references/java-style.md`; read it only when a style question actually comes up.

