Here is the comprehensive implementation plan for your Maven multi-module project. This plan integrates the new dynamic form features into your existing structure, adhering to the Java 21 and Spring Boot 3.5.6 specifications.

# Implementation Plan: Dynamic Forms & Data Persistence

This plan outlines the addition of a metadata-driven form system where structures are defined via static JSON and submissions are stored as JSON blobs.

---

## 1. Directory & Resource Setup

* **Form Definitions**: Create a directory at `backend/src/main/resources/forms/` to hold static JSON files.
* **Naming Convention**: Files should follow the pattern `{form-key}.json` (e.g., `survey.json`, `feedback.json`).
* **Dependencies**: Ensure `jackson-databind` (included in `spring-boot-starter-web`) is available for JSON parsing.

## 2. Backend: `Form` Module (Structure)

This service handles the "Blueprints" of your forms.

* **`FormService`**:
* Use `ResourcePatternResolver` to scan `classpath:/forms/*.json`.
* Load files into a `ConcurrentHashMap<String, JsonNode>` at startup to avoid repeated disk reads.


* **`FormController`**:
* `GET /api/forms`: List available form keys.
* `GET /api/forms/{key}`: Return the specific JSON definition.


* **Security**: Configure `WebSecurityConfig` to permit `GET` requests to these endpoints if forms are public, or require JWT if restricted.

## 3. Backend: `FormData` Module (Submissions)

This service handles user-submitted data using the "Schema-on-Read" approach.

* **`FormData` Entity**:
* Use Lombok `@Data`, `@NoArgsConstructor`, and `@AllArgsConstructor`.
* Use `@JdbcTypeCode(SqlTypes.JSON)` for the `data` field to support H2 and PostgreSQL JSONB.
* Include `formKey` (String) and `submittedAt` (LocalDateTime).


* **`FormDataRepository`**: Extend `JpaRepository<FormData, Long>`.
* **`FormDataController`**:
* `POST /api/form-data/{key}`: Accept a `Map<String, Object>` as the request body.
* Require OAuth2 authentication via `Authorization: Bearer <JWT>`.
* Log submissions using `@Slf4j`.



## 4. Frontend: React Module Integration

Since the frontend is packaged as a WebJar, update the React source to handle dynamic rendering.

* **Dynamic Renderer**:
* Create a component that fetches the JSON from `/api/forms/{key}`.
* Map JSON types (e.g., `text`, `number`, `select`) to corresponding React components.


* **Data Submission**:
* Collect input values into a single state object.
* Use the existing OAuth2 service to POST the state to `/api/form-data/{key}`.


* **Interceptors**: Ensure the JWT is attached to the `POST` request to pass the Backend security filters.

## 5. Database & Profiles

* **Dev/Test**: Use H2 with the `application-dev.yaml` or `application-test.yaml` profiles.
* **Production**: Ensure the `application-prod.yaml` is configured for Supabase (PostgreSQL) to handle the `JSONB` column type.

---

## 6. Execution & Build Order

1. **Define JSON**: Add your first form definition to `backend/src/main/resources/forms/`.
2. **Backend Logic**: Implement the `Form` and `FormData` packages in the `backend` module.
3. **Frontend Update**: Build the dynamic UI in the `frontend` module.
4. **Maven Build**: Run `mvn clean install` from the root to package the updated WebJar into the executable JAR.
5. **Local Test**: Use `DevelopmentApplication` to verify the full flow from rendering to DB persistence.

