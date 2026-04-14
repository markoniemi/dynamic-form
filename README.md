# Dynamic form application

A dynamic form application using multi-module Spring Boot 3 application with a modern React frontend.

## Features

- **Backend (Spring Boot)**
  - OAuth2 Authorization Server (Auth Server)
  - OAuth2 Resource Server with JPA/Hibernate
  - MapStruct for DTO mapping
  - Lombok for reduced boilerplate
  - PostgreSQL support
  - `WebTestClient` for reactive integration tests
- **Frontend (React + Vite)**
  - Packaged as a WebJar for simple deployment
  - `react-hook-form` for performant form state management
  - `zod` for schema validation
  - `react-query` for server state management
  - `vitest` for unit and integration testing
- **Testing**
  - `Testcontainers` for backend integration tests
  - `Failsafe` plugin for running integration tests (`*IT.java`)
  - `vitest` and `@testing-library/react` for frontend tests

## Project Structure

```
monolith-parent/
├── frontend/                 # React + Vite (packaged as a WebJar)
│   ├── src/                  # Source code
│   └── test/                 # Vitest test files
├── backend/                  # Spring Boot API Server (Port 8080)
│   ├── src/main/java/        # Main application source
│   └── src/test/java/        # Backend tests (Unit & Integration)
└── pom.xml                   # Root aggregator POM
```

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Node.js 20+ (or use Maven's embedded Node)
- Docker + Docker Compose (optional, for full-stack local testing)
- PostgreSQL 16+ running locally on port 5432 (if not using docker-compose)

### Local Development

#### Option 1: Backend + Frontend separately (recommended for development)

1. **Start the database**
   ```bash
   docker run --name postgres-dev -e POSTGRES_DB=template \
     -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
     -p 5432:5432 postgres:16-alpine
   ```

2. **Start Backend (Port 8080)**
   ```bash
   export SPRING_PROFILES_ACTIVE=dev
   mvn spring-boot:run -pl backend
   ```
   - Uses PostgreSQL localhost (configured in `application-dev.yaml`)
   - Database is initialized by Hibernate's `ddl-auto: update`

3. **Start Frontend (Port 5173)**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   - Vite dev server proxies `/api` requests to the backend on port 8080

#### Option 2: Full stack with docker-compose

Test the full application stack (backend, database, OAuth2 server) in Docker:

```bash
# Build the app image locally using Jib (no Dockerfile needed)
mvn -pl backend jib:dockerBuild

# Then run the full stack
docker compose up
```

- Backend runs on `http://localhost:8080`
- OAuth2 server runs on `http://localhost:9000` (for authentication)
- PostgreSQL runs on `localhost:5433` (port 5433 to avoid conflicts)
- Uses `SPRING_PROFILES_ACTIVE=prod` with environment variables (PostgreSQL, RDS-like config)

Stop with `Ctrl+C`, tear down with `docker compose down`.

### Testing

- **Run all tests (Unit, Integration, Frontend)**
  ```bash
  mvn clean verify
  ```
  - Backend: Surefire (unit tests) + Failsafe (integration tests with `*IT.java`)
  - Backend tests use the `test` profile with H2 in-memory database
  - Frontend: Vitest unit and integration tests
  - Flyway is disabled during testing; Hibernate's `ddl-auto: create-drop` handles schema

- **Run Backend tests only**
  ```bash
  mvn -pl backend verify
  ```

- **Run Frontend tests only**
  ```bash
  cd frontend
  npm test
  ```

### Build for Local Testing

Build the full project and create a runnable JAR:
```bash
mvn clean package -DskipTests
```

Run with the dev profile:
```bash
export SPRING_PROFILES_ACTIVE=dev
java -jar backend/target/backend-1.0.0-SNAPSHOT.jar
```

The application serves the frontend UI and backend API from `http://localhost:8080`.

### Build Docker Image

**Local development** — Build the image for use with docker-compose or `docker run`:

```bash
mvn -pl backend jib:dockerBuild
```

This creates `dynamic-form:1.0.0-SNAPSHOT` in your local Docker daemon. Then reference it in docker-compose or run it directly:

```bash
docker run -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/dynamicform \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e OAUTH2_ISSUER_URI=http://localhost:9000 \
  -p 8080:8080 \
  dynamic-form:1.0.0-SNAPSHOT
```

**AWS deployment** — Build and push the backend image to ECR (no local Docker daemon needed):

```bash
mvn -pl backend jib:build \
  -Djib.to.image=<AWS_ACCOUNT_ID>.dkr.ecr.eu-north-1.amazonaws.com/dynamic-form:${project.version}
```

Or use the GitHub Actions workflow in [docs/AWSDeployPlan.md](docs/AWSDeployPlan.md), which automates this.

> Requires AWS credentials configured (`aws configure` or `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` environment variables).

## Configuration

The application uses Spring Boot profiles for environment-specific configuration:

| Profile | Database | Flyway | Use case |
|---------|----------|--------|----------|
| `dev` | PostgreSQL localhost | Disabled | Local development |
| `test` | H2 in-memory | Disabled | Unit & integration tests |
| `prod` | PostgreSQL RDS (env vars) | **Enabled** | AWS deployment |
| (default) | N/A | Disabled | Will fail; you must set a profile |

Set the profile with `export SPRING_PROFILES_ACTIVE=dev` or pass `-Dspring.profiles.active=prod` to Java.

Config files:
- `application.yaml` — shared settings (server port, logging, actuator)
- `application-dev.yaml` — localhost PostgreSQL, `ddl-auto: update`, DEBUG logging
- `application-prod.yaml` — environment variable placeholders, `ddl-auto: validate`
- `application-test.yaml` — H2 in-memory, Flyway disabled, minimal logging

## Architecture

### Authentication (OAuth2 PKCE)
1. Frontend redirects to the Auth Server.
2. User logs in, and the Auth Server issues an authorization code.
3. Frontend exchanges the code for a JWT access token.
4. API requests from the frontend include the JWT in the `Authorization` header.
5. The backend (Resource Server) validates the JWT.

### Frontend State Management
- **Form State**: Managed by `react-hook-form` for performance and validation with `zod`.
- **Server State**: Managed by `react-query` for caching, refetching, and optimistic updates.

### Backend Testing Strategy
- **Unit Tests**: Standard JUnit 5 tests with Mockito (e.g., `ItemServiceTest`).
- **Repository Tests**: Use `@DataJpaTest` for fast, isolated JPA tests (e.g., `ItemRepositoryTest`).
- **Integration Tests**: Use `@SpringBootTest` with `WebTestClient` and `Testcontainers` for full-context tests (e.g., `ItemControllerTest`, `FrontendIT`).

## Deployment

### AWS

For a complete guide to deploying this application to AWS (ECS Fargate, RDS, CloudFront, etc.), see [docs/AWSDeployPlan.md](docs/AWSDeployPlan.md). The plan includes:
- Phased implementation with checkpoints
- Terraform IaC for all infrastructure
- GitHub Actions CI/CD
- Smoke tests and tear-down drills

## License

This project is provided as-is for educational and development purposes.
