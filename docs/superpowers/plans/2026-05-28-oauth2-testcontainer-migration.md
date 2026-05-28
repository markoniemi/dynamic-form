# OAuth2 Testcontainer Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate from raw GenericContainer to the new oauth-server Container class with programmatic configuration, OAuth2 client setup, and Spring @DynamicPropertySource integration.

**Architecture:** Replace the low-level GenericContainer bean with the new Container class from oauth-server library, configure test users and OAuth2 clients programmatically, use @DynamicPropertySource to inject the dynamic issuer URL into application properties, and ensure all integration tests inherit proper auth config through IntegrationTestBase.

**Tech Stack:** TestContainers 1.20.3+, jackson-dataformat-yaml 2.18.1, Spring Boot test utilities (@DynamicPropertySource), OAuth2 Container class.

---

## File Structure

**Files to modify:**
- `backend/pom.xml` — upgrade TestContainers and add jackson-dataformat-yaml
- `backend/src/test/java/com/example/backend/config/TestcontainersConfig.java` — replace GenericContainer with Container class
- `backend/src/test/java/com/example/backend/IntegrationTestBase.java` — add @DynamicPropertySource for issuer URL injection

**Files to create:**
- `backend/src/test/resources/oauth2-config.yaml` — YAML configuration for test users and clients (optional, for file-based config)

---

### Task 1: Update Dependencies

**Files:**
- Modify: `backend/pom.xml`

- [ ] **Step 1: Open pom.xml and locate testcontainers dependency**

Navigate to the `<dependencyManagement>` or `<dependencies>` section where testcontainers is declared.

- [ ] **Step 2: Upgrade TestContainers version to 1.20.3**

Find the testcontainers dependency and update the version:
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.20.3</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Add jackson-dataformat-yaml dependency**

Add this new dependency to support YAML-based config (if using file-based configuration):
```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
    <version>2.18.1</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 4: Run Maven to verify dependencies resolve**

Run: `mvn dependency:resolve`
Expected: No errors, all dependencies downloaded

- [ ] **Step 5: Commit**

```bash
git add backend/pom.xml
git commit -m "Upgrade TestContainers to 1.20.3; add jackson-dataformat-yaml 2.18.1"
```

---

### Task 2: Create OAuth2 Test Configuration YAML (Optional)

**Files:**
- Create: `backend/src/test/resources/oauth2-config.yaml`

This file supports file-based configuration. Skip if using only programmatic config.

- [ ] **Step 1: Create oauth2-config.yaml in test resources**

Create the file at `backend/src/test/resources/oauth2-config.yaml`:

```yaml
users:
  - username: testuser
    password: password
    roles:
      - USER
  - username: admin
    password: password
    roles:
      - ADMIN
      - USER

clients:
  - clientId: test-client
    clientSecret: test-secret
    redirectUris:
      - http://localhost:8080/callback
      - http://localhost:3000/callback
    scopes:
      - openid
      - profile
      - email
```

- [ ] **Step 2: Verify file is in correct location**

Run: `ls -la backend/src/test/resources/oauth2-config.yaml`
Expected: File exists

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/resources/oauth2-config.yaml
git commit -m "Add OAuth2 YAML configuration for test users and clients"
```

---

### Task 3: Migrate TestcontainersConfig to Use Container Class

**Files:**
- Modify: `backend/src/test/java/com/example/backend/config/TestcontainersConfig.java`

- [ ] **Step 1: Replace imports**

Remove the old imports and add new ones. Replace this section (lines 3-18):

```java
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
```

With:

```java
import com.github.markoniemi.oauth2server.testcontainers.Client;
import com.github.markoniemi.oauth2server.testcontainers.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
```

- [ ] **Step 2: Replace the bean definition**

Replace lines 23-34 with:

```java
  private static Container sharedContainer;

  @Bean(destroyMethod = "stop")
  public Container oAuth2ServerContainer() {
    if (sharedContainer == null) {
      sharedContainer = new Container()
          .withUser("testuser", "password", "USER")
          .withUser("admin", "password", "ADMIN", "USER")
          .withOAuth2Client(new Client("test-client", "test-secret")
              .withRedirectUri("http://localhost:8080/callback")
              .withRedirectUri("http://localhost:3000/callback")
              .withScopes("openid", "profile", "email"));
      sharedContainer.start();
    }
    return sharedContainer;
  }

  public static Container getSharedContainer() {
    return sharedContainer;
  }
```

- [ ] **Step 3: Remove the helper bean and utility method**

Delete the `ensureContainerStarted` CommandLineRunner bean (lines 36-41) and `getPortConfig` method (lines 43-49).

- [ ] **Step 4: Verify file structure**

File should now be ~40 lines total, with just @TestConfiguration class, one @Bean method, and one static getter.

- [ ] **Step 5: Verify syntax**

Run: `mvn -f backend/pom.xml clean compile`
Expected: No compilation errors

- [ ] **Step 6: Commit**

```bash
git add backend/src/test/java/com/example/backend/config/TestcontainersConfig.java
git commit -m "Migrate TestcontainersConfig to new Container class; configure test users and OAuth2 client"
```

---

### Task 4: Add @DynamicPropertySource to IntegrationTestBase

**Files:**
- Modify: `backend/src/test/java/com/example/backend/IntegrationTestBase.java`

- [ ] **Step 1: Add necessary imports**

Add these imports to the file:

```java
import com.github.markoniemi.oauth2server.testcontainers.Container;
import org.springframework.boot.test.context.DynamicPropertyRegistry;
import org.springframework.boot.test.context.DynamicPropertySource;
```

- [ ] **Step 2: Update IntegrationTestBase with @DynamicPropertySource**

Replace entire file contents with:

```java
package com.example.backend;

import com.example.backend.config.PlaywrightConfig;
import com.example.backend.config.TestcontainersConfig;
import org.springframework.boot.test.context.DynamicPropertyRegistry;
import org.springframework.boot.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import({TestcontainersConfig.class, PlaywrightConfig.class})
@ActiveProfiles("test")
public abstract class IntegrationTestBase {
  
  @DynamicPropertySource
  static void oAuth2Properties(DynamicPropertyRegistry registry) {
    var container = TestcontainersConfig.getSharedContainer();
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", 
        container::getIssuerUrl);
    registry.add("spring.security.oauth2.client.provider.oauth2.issuer-uri",
        container::getIssuerUrl);
  }
}
```

- [ ] **Step 3: Verify both files compile**

Run: `mvn -f backend/pom.xml clean compile`
Expected: No compilation errors

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/example/backend/IntegrationTestBase.java
git commit -m "Add @DynamicPropertySource to inject OAuth2 issuer URL from Container"
```

---

### Task 5: Run Integration Tests to Verify Config

**Files:**
- Test: all integration tests in `backend/src/test/java/com/example/backend/**`

- [ ] **Step 1: Run all integration tests**

Run: `mvn -f backend/pom.xml test`
Expected: All tests pass (or at least testcontainer startup succeeds)

- [ ] **Step 2: Verify OAuth2 container logs**

Check that logs show Container initialization with configured users and clients. Look for messages indicating successful startup.

- [ ] **Step 3: If tests fail, debug property injection**

If tests fail, run with debug logging:
```bash
mvn -f backend/pom.xml test -Dtest=FormControllerTest 2>&1 | grep -i "issuer\|oauth2"
```

Check that `spring.security.oauth2.resourceserver.jwt.issuer-uri` is set correctly.

- [ ] **Step 4: Commit success**

```bash
git add -A
git commit -m "Verify OAuth2 testcontainer integration tests pass with new Container config"
```

---

### Task 6: Update Any Tests That Reference OAuth2 Config Directly

**Files:**
- Modify: any test files that reference `9000` or hardcoded oauth2 URLs

- [ ] **Step 1: Search for hardcoded oauth2 references**

Run: `grep -r "9000\|authorization-server" backend/src/test/java/ || echo "No hardcoded references found"`
Expected: List of files or "No hardcoded references found"

- [ ] **Step 2: For each file found, replace hardcoded URLs**

If a test file has hardcoded URLs like `http://localhost:9000`, replace with:
```java
TestcontainersConfig.getSharedContainer().getIssuerUrl()
```

- [ ] **Step 3: Run updated tests**

Run: `mvn -f backend/pom.xml test`
Expected: Tests pass with dynamic issuer URL

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Update tests to use dynamic OAuth2 issuer URL from Container"
```

---

## Self-Review Checklist

✅ **Spec coverage:**
- Update dependencies ✅ (Task 1)
- Replace GenericContainer with Container class ✅ (Task 3)
- Configure test users and OAuth2 clients ✅ (Task 3)
- Use @DynamicPropertySource for Spring property injection ✅ (Task 4)
- Verify integration ✅ (Task 5)
- Update dependent tests ✅ (Task 6)

✅ **No placeholders** — all code examples are complete, all commands are exact

✅ **Type consistency** — Container class used throughout, Client class for OAuth2 setup, getIssuerUrl() method referenced correctly
