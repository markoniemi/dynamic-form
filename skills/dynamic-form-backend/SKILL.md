---
name: dynamic-form-backend
description: Expert knowledge for the Dynamic Form backend (Spring Boot, Java, PostgreSQL). Use this skill when working on backend code, fixing bugs, or adding features to the API.
---

# Dynamic Form Backend

## Overview

This skill provides context and workflows for the `backend` module of the Dynamic Form application. It is a Spring Boot 3.5.6 application serving as an OAuth2 Resource Server.

## Technology Stack

- **Framework:** Spring Boot 3.5.6
- **Language:** Java 17+
- **Security:** Spring Security OAuth2 Resource Server
- **Data:** Spring Data JPA, Hibernate, PostgreSQL (Prod), H2 (Dev/Test)
- **Utilities:** MapStruct (DTO mapping), Lombok (boilerplate reduction)
- **Testing:** JUnit 5, Mockito, Testcontainers, WebTestClient

## Project Structure

The backend follows a standard Maven/Spring Boot directory structure:

- `src/main/java/com/example/backend/`: Root package
  - `controller/`: REST controllers (API endpoints)
  - `service/`: Business logic services
  - `repository/`: Spring Data JPA repositories
  - `entity/`: JPA entities (Database models)
  - `dto/`: Data Transfer Objects (API models)
  - `mapper/`: MapStruct mappers
  - `config/`: Configuration classes (Security, Web, etc.)
- `src/main/resources/`:
  - `application.yaml`: Main configuration
  - `application-dev.yaml`: Development profile (H2)
  - `application-test.yaml`: Test profile
- `src/test/java/`:
  - `com/example/backend/IntegrationTestBase.java`: Base class for integration tests (Testcontainers setup)

## Development Workflow

### Running the Application

To run the application locally with the `dev` profile (using in-memory H2 database):

```bash
cd backend
mvn spring-boot:run
```

The API will be available at `http://localhost:8080/api`.

### Testing

**Unit Tests:**
Run standard unit tests:
```bash
mvn test
```

**Integration Tests:**
Run integration tests (requires Docker for Testcontainers):
```bash
mvn verify
```

### Database

- **Production:** PostgreSQL
- **Development/Test:** H2 (in-memory)
- **Schema Management:** Hibernate `ddl-auto` is used (validate in prod, update/create-drop in dev/test).

## Coding Conventions

- **DTOs:** Always use DTOs for API requests and responses. Never expose Entities directly.
- **Mappers:** Use MapStruct interfaces for converting between Entities and DTOs.
- **Dependency Injection:** Use constructor injection (Lombok `@RequiredArgsConstructor`).
- **Validation:** Use Jakarta Validation annotations (`@NotNull`, `@Size`, etc.) on DTOs.
- **Error Handling:** Use `@ControllerAdvice` / `@ExceptionHandler` for consistent error responses.

