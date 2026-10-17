# Prepare dynamic-form for the JavaFX client

The desktop client lives in `dynamic-form-javafx` (see its `docs/DynamicFormDesktopPlan.md`). It must not copy
code from this repo. This document lists what dynamic-form has to publish so the client can depend on it, the
same way this repo already depends on `oauth2-server`:

| Artifact | Where | Used by the client for |
|----------|-------|------------------------|
| `dynamic-form-api` jar | Repsy Maven repo | Shared DTOs (`FormDto`, `FieldDto`, ...) |
| `dynamic-form-testcontainers` jar | Repsy Maven repo | Start backend + auth server in client tests |
| `dynamic-form` image | GHCR | The backend container the testcontainers module starts |

## 1. Target module layout

```
dynamic-form/
├── api/                     NEW  – DTOs only, no Spring
├── backend/                      – depends on api
├── frontend/
├── testcontainers/          NEW  – DynamicFormContainer / DynamicFormStack
└── pom.xml                       – adds modules + distributionManagement
```

Module order in the root `pom.xml`: `api`, `frontend`, `backend`, `testcontainers`. The testcontainers module
needs only the image, not the backend classes, so it must not depend on `backend` (same rule as
`auth-server-testcontainers`, which "does not depend on server code").

## 2. `dynamic-form-api` jar

**Content** – move from `backend/.../dto` to `api` (package `com.example.dynamicform.api`, or keep
`com.example.backend.dto` for a smaller first change; decide once, renaming later breaks clients):

- `FormDto`, `FieldDto`, `FieldOptionDto`, `FormListItemDto`, `FormDataDto`, `ValidationErrorDto`

**Rules**
- Dependencies: `jakarta.validation-api` (the `@NotBlank`/`@Pattern` rules are useful for client-side checks)
  and Lombok as `provided`. Nothing from Spring, JPA or MapStruct.
- Backend uses Jackson 3 (Spring Boot 4). The client may use Jackson 2. Do not put Jackson types or modules in
  the api jar. Jackson annotations (`com.fasterxml.jackson.annotation`) are shared by both versions and are
  allowed, but plain classes already serialize fine, so prefer none.
- `@Value` + `@Builder` DTOs have no default constructor, so check that deserialization works in both Jackson
  versions (Lombok `@Jacksonized`, or convert to records). Add a round-trip test in `api` with a plain Jackson 2
  `ObjectMapper` to guard the wire format the client relies on.
- Entities and mappers stay in `backend`; `backend/pom.xml` gets a dependency on `dynamic-form-api`.
- Add the role names / endpoint paths as constants (`ApiPaths.FORMS = "/api/forms"`, `Roles.ADMIN`) so the client
  and backend cannot drift.

**Versioning** – the api jar and the image share the project version. A client built against api `X` runs against
image `X`.

## 3. `dynamic-form-testcontainers` jar

Mirrors `oauth2-server/auth-server-testcontainers`.

**Dependencies** (all compile scope so consumers get them transitively; consumer adds this jar as `test` scope):
`org.testcontainers:testcontainers`, `com.example:auth-server-testcontainers` (from Repsy). Optionally
`testcontainers-postgresql`.

**API sketch**

```java
// Backend alone, caller supplies the auth settings
public class DynamicFormContainer extends GenericContainer<DynamicFormContainer> {
  public DynamicFormContainer();                         // ghcr.io/markoniemi/dynamic-form:<jar version>
  public DynamicFormContainer(DockerImageName image);    // pinned or local tag
  public DynamicFormContainer withIssuerUri(String uri);        // OAUTH2_ISSUER_URI
  public DynamicFormContainer withJwkSetUri(String uri);        // ..._JWT_JWK_SET_URI
  public String getBackendUrl();                         // http://localhost:<mapped 8080>
}

// Whole stack: network + auth server + backend, wired like docker-compose.yaml
public class DynamicFormStack implements AutoCloseable {
  public DynamicFormStack();                             // default users admin/admin, user/user
  public DynamicFormStack withAuthConfigFile(String classpathResource);  // extra clients, e.g. desktop-client
  public DynamicFormStack start();
  public String backendUrl();
  public String authUrl();                               // http://localhost:9000
}
```

**Behavior the stack must implement** (all of it exists today in `docker-compose.yaml` and
`TestcontainersConfig`; the module just packages it):
- One shared `Network`; auth server gets alias `auth`.
- Auth server bound to fixed host port **9000** with `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER=http://localhost:9000`,
  because the `iss` claim must equal the backend's `OAUTH2_ISSUER_URI` and the client reaches the server on the host.
- Backend env: `OAUTH2_ISSUER_URI=http://localhost:9000`, `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://auth:9000/oauth2/jwks`.
- Database: the image defaults to profile `prod` (Postgres + Flyway). Either start a `PostgreSQLContainer` on the
  network and pass `DB_URL/DB_USERNAME/DB_PASSWORD`, or run with `SPRING_PROFILES_ACTIVE=test` (H2,
  `create-drop`). **Open point:** `application-test.yaml` has no issuer property and it is not verified that the
  `test` profile seeds the `forms/*.json` samples (`DatabaseInitializer`). Decide in the module's first IT; if the
  test profile does not fit, use Postgres. Offer `withPostgres()` / `withH2()` only if both are needed.
- Wait strategy: `Wait.forHttp("/actuator/health")` (actuator exposes `health`), 2 minute timeout.
- Allow `withLogConsumer(...)` so the client's tests can print backend logs.
- The user's auth config file is merged on top of the default users, so the stack can add `desktop-client`
  without the caller repeating `admin`/`user`. Remember the oauth2-server rule: lists replace, maps merge.

**Tests in the module (`*IT`, Failsafe)** – start the stack, assert `/api/config/oauth2-issuer-uri` returns
`http://localhost:9000`, get a token for `admin`, and assert `GET /api/forms` is 200 and `POST /api/forms` is 403
for `user`. Also use the module from `backend`'s existing ITs later, replacing `TestcontainersConfig` (optional).

## 4. Publishing

### 4.1 Maven artifacts to Repsy

Same repo as oauth2-server: `https://repo.repsy.io/mvn/markoniemi/maven`.

Root `pom.xml`:

```xml
<distributionManagement>
  <repository>
    <id>repsy</id>
    <url>https://repo.repsy.io/mvn/markoniemi/maven</url>
  </repository>
</distributionManagement>
```

- `frontend` and `backend` must not be deployed as libraries: set `<maven.deploy.skip>true</maven.deploy.skip>` in
  both so only `api`, `testcontainers` and the parent POM are published. (Publishing the parent is required, the
  children reference it.)
- The `backend` module already declares the Repsy `<repositories>` entry for `interface-log` and
  `auth-server-testcontainers`. Add the same entry to `testcontainers/pom.xml`, or move it to the parent.
- **SNAPSHOT policy:** artifacts are currently `1.0.0-SNAPSHOT`. Repsy accepts snapshots only if the repo is
  configured for it (check the repo's snapshot setting once). The client then uses `1.0.0-SNAPSHOT` and builds are
  not reproducible. Cleaner for later: release versions, `mvn versions:set` from a git tag.

### 4.2 Image to GHCR

Use the same Jib pattern as oauth2-server's `auth-server/pom.xml`: a `ci` profile that binds `jib:build` to the
`deploy` phase, so push happens only after tests pass.

In `backend/pom.xml` change the Jib target from a local name to the registry name:

```xml
<to>
  <image>ghcr.io/markoniemi/dynamic-form:${project.version}</image>
  <tags><tag>latest</tag></tags>
</to>
```

and add

```xml
<profiles>
  <profile>
    <id>ci</id>
    <build>
      <plugins>
        <plugin>
          <groupId>com.google.cloud.tools</groupId>
          <artifactId>jib-maven-plugin</artifactId>
          <executions>
            <execution>
              <id>jib-push</id>
              <phase>deploy</phase>
              <goals><goal>build</goal></goals>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

Things to update where the local image name `dynamic-form:1.0.0-SNAPSHOT` is used: `docker-compose.yaml` (`app`
service), `README.md`, `DEPLOY.md`, `deploy.sh`. Either point them at the GHCR name or keep a local name for
`jib:dockerBuild` through a property. Check `deploy.sh`/Terraform before changing the name; AWS deployment may
reference the image.

Make the GHCR package public (package settings) so clients and CI of other repos can pull it without login.

### 4.3 GitHub Actions

Adapt `.github/workflows/maven-build.yaml` to the oauth2-server shape: test everything first, publish only on a
push to the default branch.

```yaml
name: Maven Build

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
  repository_dispatch:
    types: [oauth2-server-built]

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-java@v5
        with: { distribution: temurin, java-version: '21' }

      # (keep the existing Playwright apt-get step)

      - name: Configure Maven Settings
        run: |
          mkdir -p ~/.m2
          cat <<EOF > ~/.m2/settings.xml
          <settings>
            <servers>
              <server>
                <id>repsy</id>
                <username>\${env.MAVEN_USERNAME}</username>
                <password>\${env.MAVEN_PASSWORD}</password>
              </server>
            </servers>
          </settings>
          EOF

      # Tests run for every module before anything is published
      - name: Build and test
        run: mvn -B install
        env:
          MAVEN_USERNAME: ${{ secrets.REPSY_USER }}
          MAVEN_PASSWORD: ${{ secrets.REPSY_TOKEN }}

      - name: Publish artifacts and image
        if: github.event_name == 'push'
        run: >
          mvn -B -Pci deploy -DskipTests
          -Djib.to.auth.username=${{ github.actor }}
          -Djib.to.auth.password=${{ secrets.PACKAGE_UPDATE_TOKEN }}
        env:
          MAVEN_USERNAME: ${{ secrets.REPSY_USER }}
          MAVEN_PASSWORD: ${{ secrets.REPSY_TOKEN }}

      - name: Trigger dynamic-form-javafx build
        if: success() && github.event_name == 'push'
        run: |
          gh api repos/markoniemi/dynamic-form-javafx/dispatches \
            --input - <<'EOF'
          {"event_type": "dynamic-form-built", "client_payload": {}}
          EOF
        env:
          GH_TOKEN: ${{ secrets.GH_PAT }}
```

Notes
- The existing workflow triggers on every push, not just the default branch. Restricting to `main` for publishing
  matches oauth2-server. The branch is `main` here, `master` there.
- `-DskipTests` on the deploy step is safe because the previous step ran all tests. `-Pci deploy` runs
  `maven-deploy-plugin` (to Repsy) and `jib:build` (to GHCR) in one pass.
- Secrets needed in this repo: `REPSY_USER`, `REPSY_TOKEN`, `PACKAGE_UPDATE_TOKEN` (PAT with `write:packages`),
  `GH_PAT` (to dispatch the client build). The first three already exist in oauth2-server; add them here.
  `GITHUB_TOKEN` with `packages: write` can replace `PACKAGE_UPDATE_TOKEN` for pushing to a package owned by this
  repo, but the oauth2-server precedent uses a PAT, so keep it consistent unless it causes trouble.
- The existing `oauth2-server-built` dispatch keeps working: a new auth server image re-runs this build, which
  re-runs the testcontainers module's ITs against the new server.
- The `testcontainers` module's ITs pull `ghcr.io/markoniemi/dynamic-form`, but during the build the image is not
  pushed yet (push happens after tests). Two ways out: build the image locally first with
  `jib:dockerBuild` in the same `install` run (bind it to `pre-integration-test` in `backend`, tag
  `${project.version}`) and let the module use that local tag, or run the module's ITs only in the publish step.
  Prefer the first, it tests exactly what gets pushed.

## 5. Order of work

1. Create `api` module, move DTOs, add Jackson 2 round-trip test, keep backend green. No behavior change.
2. Add `distributionManagement`, `maven.deploy.skip` on `frontend`/`backend`, Repsy server config. Publish `api`
   by hand once (`mvn -pl api -am deploy`) to check credentials and the snapshot policy.
3. Change Jib target to `ghcr.io/...`, add `ci` profile; push once by hand, make the package public.
4. Create `testcontainers` module with `DynamicFormContainer`, `DynamicFormStack` and its ITs (resolves the
   open `test` vs `prod` profile question).
5. Update the workflow, add secrets, verify the green run publishes all three artifacts.
6. Update `docker-compose.yaml`, `README.md`, `DEPLOY.md`, `deploy.sh` for the new image name.
7. In `dynamic-form-javafx`, replace its hand-written model classes with `dynamic-form-api` and its own stack
   code with `dynamic-form-testcontainers`, and add the `dynamic-form-built` trigger to its workflow.

## 6. Open questions

- Package name for the api jar: decided, `com.example.dynamicform.api`.
- Snapshots or real releases on Repsy?
- Does anything in `deploy.sh`/Terraform rely on the local image name?
- Is Postgres required in the stack, or is the H2 `test` profile enough for client tests?
