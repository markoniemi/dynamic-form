# Maven Multi-Module Spring Boot Monolith with OAuth2 - Requirement Specification

## 1. Maven Project Structure Setup

- Create root aggregator pom.xml with Java 21 and Spring Boot 3.5.6 parent configuration
- Define three modules: auth-server, frontend, backend
- Configure shared properties for Spring Boot 3.5.6, Lombok, and WebJars dependencies
- Set up Maven compiler plugin for Java 21 with UTF-8 encoding
- Add frontend module as packaging type "jar" with npm integration
- Add backend module as packaging type "jar" with spring-boot-maven-plugin for executable JAR
- Configure Spring Boot BOM for consistent dependency versions

## 2. Auth Server Module (Port 9000)

- Create auth-server module with spring-boot-starter-oauth2-authorization-server dependency
- Add SLF4J logging starter (included by default)
- Create application.yaml with fixed user credentials defined inline (username, password, roles)
- Register single frontend client with Authorization Code Flow and PKCE support
- Configure RegisteredClientRepository bean with client_secret_post disabled for public client
- Set frontend client redirect URI to http://localhost:8080/callback
- Create AuthorizationServerConfig class with SecurityFilterChain for OAuth2 endpoints
- Configure token settings: access token TTL (typically 1 hour) and refresh token TTL
- Set up authorization server issuer URL pointing to http://localhost:9000
- Create simple login controller to display hardcoded user credentials
- Add CORS configuration for development to accept localhost:8080 requests

## 3. Frontend Module (WebJars Distribution)

- Create frontend module in current React project location with pom.xml
- Keep existing React/Vite/TypeScript source code structure
- Add frontend-maven-plugin for Node.js and npm installation
- Configure npm build to produce dist directory
- Create maven-assembly-plugin configuration to package dist as WebJars JAR artifact
- Use naming convention: place frontend dist files under META-INF/resources/webjars/app/
- Update vite.config.ts to set base path to /webjars/app/
- Add proxy for /api to http://localhost:8080
- Add proxy for /login/oauth2 to http://localhost:9000
- Create OAuth2 authentication service class with PKCE support
- Implement PKCE helper: generate code_verifier, compute code_challenge
- Create login handler that redirects to auth-server with correct parameters
- Create callback component to handle authorization code exchange
- Implement token storage in React context/Redux using in-memory state
- Add request interceptor to include JWT token in Authorization header
- Create logout functionality that clears in-memory tokens
- Build frontend module to produce WebJars JAR with frontend-maven-plugin

## 4. Backend Module (Port 8080 - Main Executable)

- Create backend module with spring-boot-starter-web dependency
- Add spring-boot-starter-oauth2-resource-server for JWT validation
- Add spring-boot-starter-security for SecurityFilterChain
- Add spring-boot-starter-data-jpa for database access
- Add Lombok dependency (spring-boot-starter-lombok not available, use regular lombok with provided scope)
- Add H2 Database dependency for testing (scope: test)
- Add PostgreSQL driver for Supabase production (scope: runtime)
- Add frontend module as dependency (packaging: jar from WebJars)
- Create application.yaml for production profile with Supabase connection
- Create application-dev.yaml with H2 database configuration
- Create application-test.yaml with H2 test database configuration
- Configure OAuth2 resource server to validate JWTs from auth-server issuer
- Create WebSecurityConfig class with SecurityFilterChain
- Configure security chain to permit GET /**, POST /**, PUT /**, DELETE /** for static resources (webjars)
- Require OAuth2 authentication only for endpoints starting with /api/
- Add CORS configuration bean for development (allow localhost:3000 during dev, restrict in production)
- Create example entity with Lombok annotations: @Data, @Entity, @NoArgsConstructor, @AllArgsConstructor
- Create JPA repository for example entity
- Create example REST controller with GET /api/data endpoint that returns example data
- Use @Slf4j annotation from Lombok for logging in services and controllers
- Add maven-resources-plugin if needed for any static resource copying
- Configure spring-boot-maven-plugin for executable JAR creation

## 5. DevelopmentApplication for Local Testing

- Create src/test/java/com/example/DevelopmentApplication class
- Add @SpringBootTest and @ActiveProfiles("test") annotations
- Use testcontainers-spring-boot-test for OAuth server container
- Create embedded testcontainer instance of auth-server module
- Configure testcontainer to run auth-server on port 9000 during test
- Set up H2 database for backend module in test profile
- Implement static method or @Bean to start both services programmatically
- Create main method to allow running as standalone application for development
- Configure logging to SLF4J for both services during test execution
- Set up environment variables for testcontainer configurations

## 6. Database and Entity Layer

- Create simple example entity (e.g., Task, Note, or Item) with Lombok annotations
- Use @Data for getter/setter generation, @Entity for JPA mapping, @NoArgsConstructor, @AllArgsConstructor
- Add @Id and @GeneratedValue for primary key
- Add timestamp fields with @CreationTimestamp and @UpdateTimestamp (if using hibernate-commons-annotations)
- Create JpaRepository interface extending JpaRepository<Entity, Long>
- Add basic finder methods if needed (e.g., findByUserId, findAll)
- Use SLF4J with @Slf4j for repository logging
- Configure Hibernate dialect for H2 in test and PostgreSQL in production

## 7. OAuth2 and Security Configuration

- Configure JWT validation with proper issuer and audience claims
- Set up token extraction from Authorization: Bearer header
- Create custom JwtAuthenticationConverter if custom claims need mapping
- Configure GrantedAuthoritiesMapper to extract authorities from JWT scopes
- Set authentication as stateless since SPA uses JWT in-memory
- Configure CORS bean to handle preflight requests in development
- Add WebSecurityCustomizer to exclude static resources from security
- Create @RestControllerAdvice for exception handling with proper HTTP status codes
- Return 401 for unauthorized, 403 for forbidden, 404 for not found resources

## 8. Build Pipeline and Maven Integration

- Configure root pom.xml build order: auth-server first, frontend second, backend third
- Ensure frontend module completes npm build before backend dependency resolution
- Set frontend dependency with classifier "webjars" in backend pom.xml
- Configure spring-boot-maven-plugin to create executable JAR with webjars embedded
- Create Maven wrapper (mvn.cmd and .mvn directory) for consistent builds
- Configure jacoco-maven-plugin for test coverage if desired
- Set maven-surefire-plugin to run tests in test phase
- Verify backend module finds frontend WebJars resources at classpath:META-INF/resources/webjars/

## 9. Development Workflow

- Create separate startup commands documentation
- Standalone auth-server: cd auth-server && mvn spring-boot:run (port 9000)
- Standalone backend: cd backend && mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev (port 8080)
- Frontend development: cd frontend && npm run dev (port 5173 with proxies to backends)
- Integration testing: Run DevelopmentApplication from IDE or mvn test
- Production build: mvn clean install from root to create backend/target/backend-X.X.X.jar

## 10. Production Configuration and Documentation

- Create application-prod.yaml with Supabase PostgreSQL connection
- Configure connection pooling: HikariCP with max pool size 10
- Set up proper SSL configuration for HTTPS in production
- Document environment variables: SUPABASE_DB_URL, SUPABASE_DB_PASSWORD
- Create Docker configuration (Dockerfile) to run backend JAR if needed
- Document deployment process: run single JAR to start both API and frontend UI
- Create startup script documentation for systemd or container orchestration
- Add health check endpoints: GET /actuator/health for Kubernetes probes

## 11. Logging and Monitoring

- Configure SLF4J with Logback as default implementation
- Use @Slf4j from Lombok in all service, controller, and repository classes
- Configure different log levels for development vs production profiles
- Add request/response logging interceptor at INFO level
- Log authentication events at WARN level
- Add performance logging for database queries (DEBUG level in dev, OFF in prod)
- Create logback-spring.xml for profile-specific logging configuration

## 12. Exception Handling and Validation

- Create custom exception classes for application-specific errors
- Create @RestControllerAdvice for centralized exception handling
- Log exceptions using SLF4J @Slf4j at appropriate levels (ERROR for unexpected)
- Return meaningful error messages to client with proper HTTP status codes
- Add input validation using javax.validation annotations on entities
- Validate JWT claims (exp, iat, iss) in security configuration

## Key Architectural Changes

- Frontend is now packaged as WebJars and served from backend module
- No separate frontend build/deployment needed - single JAR contains everything
- H2 database for quick testing without external dependencies
- DevelopmentApplication enables easy local testing with both services
- In-memory JWT storage eliminates cookie/session complexity for SPA
- Lombok reduces boilerplate for entities and logging
- SLF4J provides unified logging across all modules

## Summary

This refined approach creates a tightly integrated monolith where the frontend is packaged as WebJars within the backend module. Three Maven modules (auth-server, frontend, backend) build together into a single executable JAR containing both API and UI. DevelopmentApplication with testcontainers enables seamless local development and testing. Using Lombok and SLF4J throughout minimizes boilerplate code. The in-memory JWT storage in React combined with H2 testing database creates a lightweight, efficient development experience while maintaining production readiness with Supabase PostgreSQL.
