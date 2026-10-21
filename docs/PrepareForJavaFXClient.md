# dynamic-form as a dependency of the JavaFX client

**Status: done.** All three artifacts are published and the desktop client
(`dynamic-form-javafx`, see its `docs/DynamicFormDesktopPlan.md`) consumes them. This document started as a
to-do list; it is now the record of what the published contract actually is, plus the actions still outstanding
in section 6. The client must not copy code from this repo.

| Artifact | Where | Version | Used by the client for |
|----------|-------|---------|------------------------|
| `dynamic-form-api` jar | Repsy Maven | `1.0.0-SNAPSHOT` | Shared DTOs (`FormDto`, `FieldDto`, ...) |
| `dynamic-form-testcontainers` jar | Repsy Maven | `1.0.0-SNAPSHOT` | Start backend + auth server in client tests |
| `dynamic-form` image | GHCR | `1.0.0-SNAPSHOT`, `latest` | The backend container the testcontainers module starts |

Repsy is `https://repo.repsy.io/mvn/markoniemi/maven` and is public for reads, so a client needs no credentials
to resolve the jars. All three were last published 2026-10-17. **The GHCR package is still private — see
section 6.**

## 1. Module layout

```
dynamic-form/
├── api/                     – DTOs only, no Spring
├── backend/                 – depends on api
├── frontend/
├── testcontainers/          – DynamicFormContainer / DynamicFormStack
└── pom.xml                  – modules + distributionManagement
```

Module order in the root `pom.xml` is `api`, `frontend`, `backend`, `testcontainers`. The testcontainers module
needs only the image, not the backend classes, so it does not depend on `backend` (same rule as
`auth-server-testcontainers`, which does not depend on server code).

## 2. `dynamic-form-api`

Package `com.example.dynamicform.api`. Six DTOs: `FormDto`, `FieldDto`, `FieldOptionDto`, `FormListItemDto`,
`FormDataDto`, `ValidationErrorDto`.

`FormDto`, `FieldDto` and `FieldOptionDto` carry `@Value @Builder @AllArgsConstructor`; `FormListItemDto`,
`FormDataDto` and `ValidationErrorDto` carry `@Value` only, so they have an all-args constructor and no builder.
`FormDataDto` is `Serializable`, has `@NonNull` on `formKey` and `data`, and its `submittedAt` is a
`java.time.LocalDateTime`.

**Rules that must hold, because clients depend on them**

- Dependencies: `jakarta.validation-api` (the `@NotBlank`/`@Pattern` rules are useful for client-side checks)
  and Lombok as `provided`. Nothing from Spring, JPA or MapStruct. Jackson is `test` scope only.
- The backend uses Jackson 3 (Spring Boot 4); clients may use Jackson 2. No Jackson types or modules belong in
  this jar. Jackson annotations are shared by both versions and would be allowed, but plain classes serialize
  fine, so there are none.
- **`api/lombok.config` sets `lombok.anyConstructor.addConstructorProperties = true` and is load-bearing.** It
  makes Lombok emit `@java.beans.ConstructorProperties` on the generated constructors, which is the only reason
  a plain Jackson 2 `ObjectMapper` can construct these `@Value` classes — they have no no-arg constructor.
  Delete that line and every Jackson 2 client breaks. This is what was chosen instead of `@Jacksonized` or
  records.
- A client needs `jackson-datatype-jsr310` (`JavaTimeModule`) because of `FormDataDto.submittedAt`. Without it
  deserialization fails. Worth stating in client-facing docs; it is not optional.
- `JacksonRoundTripTest` guards all of the above with a plain Jackson 2 `ObjectMapper`. Keep it green; it is the
  wire-format contract.
- Entities and mappers stay in `backend`, which depends on this jar.

**Not in the jar, contrary to an earlier draft of this section:** there is no `ApiPaths` and no `Roles` class.
An earlier version of this document promised them and they were never built, which misled the client's planning.
They remain a good idea and are now proposed from the client side — see section 5.

**Versioning:** the api jar and the image share the project version. A client built against api `X` runs against
image `X`.

## 3. `dynamic-form-testcontainers`

Mirrors `oauth2-server/auth-server-testcontainers`. Dependencies are compile scope so consumers get them
transitively (the consumer adds this jar as `test` scope): `org.testcontainers:testcontainers`,
`org.testcontainers:testcontainers-postgresql`, `com.example:auth-server-testcontainers:0.1-SNAPSHOT`. The
module declares the Repsy `<repositories>` entry and runs its own ITs under Failsafe.

### `DynamicFormContainer`

```java
public class DynamicFormContainer extends GenericContainer<DynamicFormContainer> {
  public static final int BACKEND_PORT = 8080;
  public DynamicFormContainer();                       // ghcr.io/markoniemi/dynamic-form:<this jar's version>
  public DynamicFormContainer(DockerImageName image);  // pinned or local tag
  public DynamicFormContainer withIssuerUri(String uri);                       // OAUTH2_ISSUER_URI + ..._JWT_ISSUER_URI
  public DynamicFormContainer withJwkSetUri(String uri);                       // ..._JWT_JWK_SET_URI
  public DynamicFormContainer withDatabase(String jdbcUrl, String user, String password);  // DB_URL/DB_USERNAME/DB_PASSWORD
  public String getBackendUrl();                       // http://localhost:<mapped 8080>
}
```

The default constructor reads the version from `/dynamicform/version.properties`, which the module generates by
resource filtering — that is what keeps the jar and the image on the same version. The container sets
`SPRING_PROFILES_ACTIVE=prod` and waits for `/actuator/health` to return 200, with a 2 minute startup timeout.
The image has no H2, so the caller must supply a database.

### `DynamicFormStack`

```java
public class DynamicFormStack implements AutoCloseable {
  public DynamicFormStack();                                             // default users admin/admin, user/user
  public DynamicFormStack withAuthConfigFile(String classpathResource);  // extra clients, e.g. desktop-client
  public DynamicFormStack withLogConsumer(Consumer<OutputFrame> consumer);
  public DynamicFormStack start();
  public String backendUrl();
  public String authUrl();                                               // http://localhost:9000
  public void close();
}
```

What it wires, matching `docker-compose.yaml`:

- One shared `Network`. The auth server gets alias `auth`, Postgres gets alias `db`.
- The auth server is bound to **fixed host port 9000** with issuer `http://localhost:9000`, because the `iss`
  claim must equal the backend's issuer and clients reach the server on the host. Consequence: tests cannot run
  beside a local auth server on 9000.
- Backend env: issuer `http://localhost:9000`, JWK set `http://auth:9000/oauth2/jwks`.
- **Database: Postgres** (`postgres:16-alpine`), on the network, passed to the backend via `withDatabase`. The
  earlier open question about the H2 `test` profile was resolved in favour of `prod` — Flyway migrations run and
  the `forms/*.json` samples are seeded, which client tests rely on. No `withPostgres()`/`withH2()` switch
  exists, because only one is needed. The cost is a third image pull and a slower start than H2.
- Default users come from `dynamicform/auth-defaults.yaml` in this jar: `admin/admin [USER, ADMIN]` and
  `user/user [USER]`.
- `withAuthConfigFile` copies the given classpath resource to `/config/extra/application.yaml` and sets
  `SPRING_CONFIG_ADDITIONAL_LOCATION`, so it layers **over** the defaults. The oauth2-server merge rule applies:
  maps such as clients merge by key, while single values and lists are replaced — so a file that supplies a
  `users` list replaces the default users rather than adding to them.
- `close()` stops backend, database and auth server and closes the network.

### The `desktop-client` registration

`testcontainers/src/test/resources/desktop-client.yaml` registers the desktop client used by
`DynamicFormStackIT`: public client (`client-authentication-methods: [none]`), `authorization_code`,
`require-proof-key: true`, scopes `openid profile email`, 1h access token, and redirect URI
**`http://localhost:8123/callback`** — a fixed port.

The JavaFX client matches that registration exactly rather than using a dynamic loopback port, so the fixed port
is now part of the contract in practice. It lives in this module's *test* resources, so the client keeps its own
copy; if it ever moves to `src/main/resources` it becomes something clients can reuse directly.

`DynamicFormStackIT` also contains a working hand-rolled authorization-code + PKCE flow against this server
(login form scrape, `_csrf`, redirect chain, code exchange). It is the reference implementation for any client
that needs a token headlessly — read it rather than deriving the redirect sequence again.

## 4. Publishing

### 4.1 Maven artifacts to Repsy

`distributionManagement` in the root `pom.xml` points at Repsy. `frontend` and `backend` both set
`<maven.deploy.skip>true</maven.deploy.skip>`, so only `api`, `testcontainers` and the parent POM are published
(the parent is required — the children reference it).

**Snapshots are confirmed working:** Repsy serves them, and all three artifacts are live at `1.0.0-SNAPSHOT`.
The trade-off stands that `-SNAPSHOT` builds are not reproducible; release versions driven from a git tag
(`mvn versions:set`) are the cleaner end state and are still open on the client side.

### 4.2 Image to GHCR

`backend/pom.xml` targets `ghcr.io/markoniemi/dynamic-form:${project.version}` plus a `latest` tag, and a `ci`
profile binds `jib:build` to the `deploy` phase so the push happens only after tests pass. `docker-compose.yaml`,
`README.md` and `DEPLOY.md` use the GHCR name; `deploy.sh` and Terraform override it with `-Djib.to.image` to
ECR, so they are unaffected.

The testcontainers module's ITs run during `install`, before anything is pushed, so the image is built locally
first with `jib:dockerBuild` and the module uses that local tag. That means the ITs test exactly what gets
pushed.

### 4.3 GitHub Actions

`.github/workflows/maven-build.yaml` runs on every push and pull request, plus `repository_dispatch` of type
`oauth2-server-built`. Every module's tests run first via `mvn clean install`. Publishing is gated on
`github.event_name == 'push' && github.ref == 'refs/heads/master'` and runs `mvn -Pci deploy -DskipTests`, which
deploys to Repsy and pushes to GHCR in one pass. A final step dispatches `dynamic-form-built` to
`dynamic-form-javafx`, with `continue-on-error: true` until that repo has a workflow listening for it.

Secrets used: `REPSY_USER`, `REPSY_TOKEN`, `PACKAGE_UPDATE_TOKEN` (PAT with `write:packages`) and `GH_PAT` for
the dispatch.

Note the default branch here is `master`; `dynamic-form-javafx` uses `main`.

## 5. Proposed, not built

Raised from the client side and worth doing here, since this is where the api jar lives:

- **`ApiPaths` and `Roles` constants in the api jar.** `ApiPaths.FORMS = "/api/forms"`, `Roles.ADMIN`, used by
  both this repo's annotations and the client, so neither can drift on a string literal. Zero dependency cost.
  The backend currently repeats these literals in `@RequestMapping`, `@PreAuthorize` and `SecurityUtils`.
  Shape note: Spring splits class-level and method-level paths and annotation values must be compile-time
  constants, so the jar wants both the raw templates (`FORMS = "/api/forms"`, `BY_KEY = "/{key}"`) and composing
  helpers (`form(formKey)`) for clients.
- **Annotation-free operation interfaces** (`FormOperations`, `FormDataOperations`) in the api jar, carrying
  DTO-based signatures with no security parameters, for clients to implement. This is the
  `microservice-example` pattern. Be clear about what it buys: in that precedent the server does **not**
  implement the shared interface either, so it gives a written-down contract and a pre-declared client
  interface, not compile-checked server agreement.
  **Do not name them `FormService`/`FormDataService`** — those names are taken in
  `com.example.backend.service`, and those classes are entity-based (`FormService.get(String) → Form`,
  `FormDataService.getAll() → List<FormData>`), so they could not implement a DTO-based interface of the same
  name regardless.
- **springdoc / an OpenAPI document.** There is none today. With one, clients could be generated from the spec
  instead of hand-written, which would subsume much of the above.

## 6. Outstanding actions

1. **Make the GHCR package public.** It is still private: `ghcr.io/markoniemi/dynamic-form` returns
   `UNAUTHORIZED` from the anonymous token endpoint, while `ghcr.io/markoniemi/oauth2-server` issues a token
   normally. This repo's own CI is unaffected because it authenticates to push, and local work is unaffected
   because `jib:dockerBuild` puts the image in the local daemon — but **any other repo's CI, and any developer
   not logged in to GHCR, cannot pull it**, so the JavaFX client's integration tests will fail there. Fix in the
   package settings, then re-check with:
   `curl -s "https://ghcr.io/token?scope=repository%3Amarkoniemi%2Fdynamic-form%3Apull&service=ghcr.io"`
2. Decide release versions versus `-SNAPSHOT` (section 4.1). Needed before the client ships an installer.
3. Optionally use the testcontainers module from `backend`'s own ITs, replacing `TestcontainersConfig`.
4. Consider the section 5 proposals.

## 7. Resolved questions

- Package name for the api jar: `com.example.dynamicform.api`.
- Snapshots or real releases on Repsy? Snapshots work and are in use; releases remain the cleaner end state.
- Does anything in `deploy.sh`/Terraform rely on the local image name? No — both override it with
  `-Djib.to.image` to ECR.
- Is Postgres required in the stack, or is the H2 `test` profile enough? Postgres, so that Flyway runs and the
  sample forms are seeded.
