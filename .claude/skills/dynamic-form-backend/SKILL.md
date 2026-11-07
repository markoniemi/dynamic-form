---
name: dynamic-form-backend
description: Expert knowledge for the Dynamic Form backend (Spring Boot, Java, PostgreSQL). Use this skill when working on backend code, fixing bugs, or adding features to the API.
---

# Dynamic Form Backend

## Overview

This skill provides context and workflows for the `backend` module of the Dynamic Form application. It is a Spring Boot application serving as an OAuth2 Resource Server. For the framework and language versions, see `backend/CLAUDE.md` â€” they are not repeated here.

## Technology Stack

- **Security:** Spring Security OAuth2 Resource Server
- **Data:** Spring Data JPA, Hibernate, PostgreSQL (prod and dev), H2 (test only)
- **Utilities:** MapStruct (DTO mapping), Lombok (boilerplate reduction)
- **Testing:** JUnit 5, Mockito, Testcontainers, WebTestClient

## Project Structure

The backend follows a standard Maven/Spring Boot directory structure:

- `src/main/java/com/example/backend/`: Root package
  - `controller/`: REST controllers (API endpoints)
  - `service/`: Business logic services
  - `repository/`: Spring Data JPA repositories
  - `entity/`: JPA entities (Database models)
  - `mapper/`: MapStruct mappers
  - `config/`: Configuration classes (Security, Web, etc.)
- DTOs are **not** in this module: they live in the `api` module, package `com.example.dynamicform.api`
- `src/main/resources/`:
  - `application.yaml`: Main configuration
  - `application-dev.yaml`: Development profile (Postgres)
  - `application-prod.yaml`: Production profile (Postgres)
  - `application-test.yaml`: Test profile (H2)
- `src/test/java/`:
  - `com/example/backend/IntegrationTestBase.java`: Base class for integration tests (Testcontainers setup)

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
- **Schema Management:** Flyway is enabled in `prod` only; dev and test schemas come from Hibernate. A schema change needs a new `db/migration/V<n>__*.sql` â€” never edit an applied one.

## Coding Conventions

- **DTOs:** Always use DTOs for API requests and responses. Never expose Entities directly.
- **Mappers:** Use MapStruct interfaces for converting between Entities and DTOs.
- **Dependency Injection:** Use constructor injection (Lombok `@RequiredArgsConstructor`).
- **Validation:** Use Jakarta Validation annotations (`@NotNull`, `@Size`, etc.) on DTOs.
- **Error Handling:** Throw standard Java exceptions from the service layer â€” never HTTP-specific exceptions like `ResponseStatusException`. Map all exceptions to HTTP responses once, centrally, in a `@RestControllerAdvice`:

  | Exception | HTTP status |
  |---|---|
  | `NoSuchElementException` | 404 Not Found |
  | `IllegalArgumentException` | 400 Bad Request |
  | `IllegalStateException` | 409 Conflict |
  | `SecurityException` | 403 Forbidden |

General Java and clean-code guidance is in `references/java-style.md`; read it only when a style question actually comes up.

