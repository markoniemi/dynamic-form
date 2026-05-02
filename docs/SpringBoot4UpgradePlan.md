# Spring Boot 4.0.3 Upgrade Plan

Generated: 2026-05-02  
Current Status: Not started

---

## Overview

Upgrading from Spring Boot 3.5.14 → 4.0.3. Spring Boot 4.0 uses Spring Framework 7.0, Spring Security 7.0, Hibernate 7.1, and Jackson 3 — all major library versions with breaking API and configuration changes.

---

## Step 1 — Bump Spring Boot version

**File:** `pom.xml` (root)

```xml
<!-- Change -->
<version>3.5.14</version>

<!-- To -->
<version>4.0.3</version>
```

- [ ] Update `spring-boot-starter-parent` version to 4.0.3

---

## Step 2 — Rename starters

**File:** `backend/pom.xml`

| Old artifact | New artifact |
|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-oauth2-resource-server` | `spring-boot-starter-security-oauth2-resource-server` |

- [ ] Rename `spring-boot-starter-web`
- [ ] Rename `spring-boot-starter-oauth2-resource-server`

---

## Step 3 — Remove removed properties

`spring.jpa.database-platform` is removed in Spring Boot 4.0. Hibernate 7 auto-detects `PostgreSQLDialect`.

**Files:** `backend/src/main/resources/application-dev.yaml`, `application-prod.yaml`

Remove both occurrences of:
```yaml
database-platform: org.hibernate.dialect.PostgreSQLDialect
```

- [ ] Remove from `application-dev.yaml`
- [ ] Remove from `application-prod.yaml`

---

## Step 4 — Compile and fix remaining errors

```bash
mvn -f backend/pom.xml compile -q
```

Known additional breaking changes to watch for in compile output:

- **Jackson 3**: group IDs changed from `com.fasterxml.jackson.*` → `tools.jackson.*`; check any direct Jackson imports in Java source
- **EntityScan import**: `org.springframework.boot.autoconfigure.domain.EntityScan` → `org.springframework.boot.persistence.autoconfigure.EntityScan`

- [ ] Run compile
- [ ] Fix any Jackson 3 import errors
- [ ] Fix any EntityScan import errors
- [ ] Fix any other compile errors

---

## Step 5 — Run unit tests

```bash
mvn -f backend/pom.xml test
```

- [ ] All unit tests pass

---

## Step 6 — Run full build

```bash
mvn install
```

- [ ] Full build including E2E tests passes

---

## Notes

- Testing already uses `@MockitoBean` (not `@MockBean`) — no test annotation changes needed
- `spring.mvc.problemdetails.enabled=true` in `application.properties` is now the default — can be removed but harmless to keep
- CORS: `setAllowedHeaders(List.of("*"))` with `setAllowCredentials(true)` is a CORS spec violation — consider replacing wildcard with explicit header list as a follow-up
