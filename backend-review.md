# Backend Code Review Results

**Date:** 2026-06-02  
**Scope:** Backend main source code (Java/Spring Boot)  
**Reviewed against:** dynamic-form-review skill

---

## Summary

Backend codebase is **well-structured** with clean architecture separation. Controllers delegate properly, services contain business logic, DTOs used throughout, and MapStruct mappers present. Exception handling centralized. Main findings: manual null checks where Apache Commons could help, overly permissive security config, and use of non-standard exception types for authorization checks.

---

## Must Fix

### 1. SecurityUtils.java — Manual null checks instead of Apache Commons

**File:** `backend/src/main/java/com/example/backend/util/SecurityUtils.java`

**Lines:** 11, 15, 22, 25

**Rule violated:** Use Apache Commons for null/empty checks instead of manual conditionals

**Problem:** Multiple manual null checks; Apache Commons utilities would be cleaner.

**Fix:** Use Apache Commons utilities (StringUtils, collections API)

```java
// Before
if (jwt == null) {
  return "anonymous";
}
String username = jwt.getClaimAsString("preferred_username");
if (username == null) {
  username = jwt.getSubject();
}

// After
if (jwt == null) {
  return "anonymous";
}
String username = StringUtils.defaultIfBlank(
    jwt.getClaimAsString("preferred_username"), 
    jwt.getSubject());
```

Also apply to lines 22–27 (authentication null checks) and line 25 (authorities null check).

---

### 2. SecurityConfig.java — Overly permissive route authorization

**File:** `backend/src/main/java/com/example/backend/config/SecurityConfig.java`

**Lines:** 33–34

**Rule violated:** Endpoints secured with `@PreAuthorize`; route ordering confusing

**Problem:** `/api/forms/**` route is `permitAll()` but FormController methods have `@PreAuthorize("isAuthenticated()")`. Route-level config should match method-level security.

**Fix:** Remove `/api/forms/**` from permitAll block or ensure consistency:

```java
// Current (confusing)
.requestMatchers("/api/forms/**").permitAll()  // Line 33
.requestMatchers("/api/**").authenticated()     // Line 35

// Fix - remove forms from permitAll since it's protected at method level
// Remove lines 33-34, let /api/** authenticated() catch it
```

---

### 3. SecurityConfig.java — Hardcoded CORS origins

**File:** `backend/src/main/java/com/example/backend/config/SecurityConfig.java`

**Lines:** 49–50

**Rule violated:** Config via `@Value` or `@ConfigurationProperties`, not hardcoded strings

**Problem:** Localhost origins hardcoded; should use environment variables for different environments.

**Fix:** Move to `application.properties` and inject via `@Value`:

```properties
# application.properties
app.cors.allowed-origins=http://localhost:8080,http://localhost:5173,http://localhost:9000
```

```java
@Value("${app.cors.allowed-origins}")
private String allowedOrigins;

// In corsConfigurationSource():
configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
```

---

## Should Fix

### 1. FormDataController.java — Redundant security parameters

**File:** `backend/src/main/java/com/example/backend/controller/FormDataController.java`

**Lines:** 58, 70

**Rule violated:** Clear, intention-revealing names and minimal duplication

**Problem:** Methods receive both `@AuthenticationPrincipal Jwt jwt` and `Authentication authentication`. Only one is needed; creates confusion.

**Fix:** Pick one pattern and use consistently:

```java
// Before
public List<FormDataDto> getSubmissions(
    @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
  if (isAdmin(authentication)) { ... }
  else { ... getUsername(jwt) ... }
}

// After - use only Authentication
public List<FormDataDto> getSubmissions(@AuthenticationPrincipal Authentication auth) {
  if (isAdmin(auth)) { ... }
  else { ... getUsername(extractJwt(auth)) ... }
}
```

---

## Consider

### 1. Add dependency version checks

**Files:** `backend/pom.xml`

**Rule:** Check if dependencies have newer versions or are unmaintained

**Suggestion:** Run `mvn dependency:tree` and check Maven Central for updates. No current blocking issues found.

---

### 2. Expand logging in service methods

**Files:** `FormService.java`, `FormDataService.java`

**Suggestion:** Add audit logging to `getFormSubmissionById()`, `getFormSubmissions()`, `getFormSubmissionsByOwner()` for security/compliance tracking. Currently only saveForm/updateForm/deleteForm have logs.

---

## Architecture Compliance

| Aspect | Status | Notes |
|--------|--------|-------|
| Controller delegation | ✓ | Clean separation; controllers call services only |
| Service layer | ✓ | Business logic properly placed |
| DTOs used | ✓ | All API input/output typed |
| MapStruct | ✓ | Mappers present for all entity↔DTO conversions |
| Constructor injection | ✓ | Proper use; no field @Autowired |
| Exception handling | ⚠️ | Centralized GlobalExceptionHandler; SecurityException usage issue |
| Validation | ✓ | Jakarta Validation on DTOs |
| Security | ⚠️ | @PreAuthorize used; config issues noted |

---

## Recommendations

1. **Immediate:** Apply Apache Commons to SecurityUtils.java null checks (must fix #1)
2. **Soon:** Fix route authorization config in SecurityConfig (must fix #2)
3. **Soon:** Move CORS origins to config properties (must fix #3)
4. **Follow-up:** Consolidate FormDataController auth parameters (should fix #1)
