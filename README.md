# Spring Boot & React Monolith Template

A production-ready, multi-module Spring Boot 3 monolithic application with a modern React frontend.

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
├── auth-server/              # OAuth2 Authorization Server (Port 9000)
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
- Node.js 20+

### Development

1. **Start Auth Server (Port 9000)**
   ```bash
   cd auth-server
   mvn spring-boot:run
   ```
   - **Credentials**: `admin` / `admin`

2. **Start Backend (Port 8080)**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   - Uses H2 in-memory database by default in the `dev` profile.

3. **Start Frontend (Port 5173)**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   - Vite dev server proxies `/api` to the backend on port 8080.

### Testing

- **Run Backend Tests (Unit & Integration)**
  ```bash
  cd backend
  mvn verify
  ```
  - This runs both Surefire (unit tests) and Failsafe (integration tests).

- **Run Frontend Tests**
  ```bash
  cd frontend
  npm test
  ```

### Production Build

Build all modules and package the frontend into the backend JAR:
```bash
mvn clean install
```

Run the final application:
```bash
java -jar backend/target/backend-1.0.0.jar
```
- The application serves the frontend UI and backend API from `http://localhost:8080`.
- The Auth Server must be running separately.

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

## License

This project is provided as-is for educational and development purposes.
