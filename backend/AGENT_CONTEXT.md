# Backend Module Context

## Overview
This module is the Spring Boot API server (Resource Server). It provides REST endpoints and connects to the database.

## Key Technologies
- Spring Boot 3.5.6
- Spring Security OAuth2 Resource Server
- Spring Data JPA
- Hibernate
- MapStruct
- Lombok
- PostgreSQL (Production) / H2 (Dev/Test)
- Testcontainers & WebTestClient (Testing)

## Configuration
- **Port**: 8080
- **Context Path**: `/`
- **API Prefix**: `/api`

## Key Files
- `src/main/java/com/example/backend/controller/`: REST controllers.
- `src/main/java/com/example/backend/service/`: Business logic.
- `src/main/java/com/example/backend/repository/`: Data access.
- `src/main/java/com/example/backend/entity/`: JPA entities.
- `src/main/java/com/example/backend/dto/`: Data Transfer Objects.
- `src/test/java/com/example/backend/IntegrationTestBase.java`: Base class for integration tests using Testcontainers.

## Testing Strategy
- **Unit Tests**: JUnit 5 + Mockito.
- **Repository Tests**: `@DataJpaTest`.
- **Integration Tests**: `@SpringBootTest` + `WebTestClient` + `Testcontainers`.

## Development
- Run with `mvn spring-boot:run`.
- Uses H2 in-memory DB by default in `dev` profile.
