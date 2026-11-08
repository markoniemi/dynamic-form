---
id: lombok
status: in_progress
priority: medium
kind: task
---

- lombok is not used in all places where it could. Check where lombok could be used.

# Implementation Plan

Lombok is already the house style: `@Slf4j` on every logging class, `@RequiredArgsConstructor`
on every injected bean, `@Value`/`@Builder` on the DTOs, `@Data`/`@Builder` on the entities.
A grep for `LoggerFactory`, manual getters and hand-written constructors over `api/` and
`backend/` leaves only two real gaps. The rest of this plan records what was checked and
rejected, so the question does not get re-opened.

## Do

1. `backend/src/main/java/com/example/backend/util/SecurityUtils.java` — all-static helper with
   an implicit public constructor, so it can be instantiated. Add `@UtilityClass`: makes the
   class final, the constructor private, and members static (drop the now-redundant `static`
   keywords). Callers are unchanged. `SecurityUtilsTest` covers both methods.
2. `backend/src/main/java/com/example/backend/controller/ConfigController.java` — hand-written
   constructor for a single `@Value`-injected field, the one injected bean in `backend` that is
   not `@RequiredArgsConstructor`. Move `@Value` onto the field, add `@RequiredArgsConstructor`,
   and add `backend/lombok.config` with
   `lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Value`
   so the annotation is copied onto the generated constructor parameter.

## Verify

`mvn -pl backend -am verify` from the repo root. Step 2 fails silently without the
`copyableAnnotations` line (`oauth2IssuerUri` would be injected as `null`), and that is already
covered: the SPA calls `/api/config/oauth2-issuer-uri` on load via `loadOidcConfig`, so every
`e2e/` IT breaks at login if the value is lost. No new test needed.

## Checked and rejected

* `e2e/pages/BasePage.java` and its five subclasses — Lombok cannot generate a constructor that
  passes an argument to `super(...)`, so the subclasses keep their constructors either way.
  `BasePage` alone would go from three lines to two. Not worth the churn.
* `api/` DTOs — `FormListItemDto` and `ValidationErrorDto` have `@Value` but no `@Builder`, unlike
  the other four. Both are built with `new` at a handful of call sites and read fine. Adding a
  builder for symmetry alone touches a published module for no caller's benefit. See the open
  question below.
* Loggers — no manual `LoggerFactory` or `private static final Logger` anywhere in `api/` or
  `backend/`. `@Slf4j` is already on all eight classes that log.
* `SecurityConfig`, `WebConfig`, `RoleClaimsConverter`, `BackendApplication`, `repository/`
  (Spring Data interfaces), `mapper/` (MapStruct interfaces) — no field, constructor, accessor or
  logger boilerplate for Lombok to take over.
* `FrontendIT`, `AdminRoleIT` — their constructors build page objects from the injected `Page`;
  that is logic, not field assignment.
* `testcontainers/` — the module has no Lombok dependency. Adding one to generate a handful of
  lines is not a win, and it is a published library.

## Decided

`Form` and `FormData` keep `@Data`. The plan asked whether to narrow them to `@Getter`/`@Setter`
because `@Data` generates `equals`/`hashCode` over the generated `id` and `Form.fields` — the
Hibernate identity pitfall. The human's answer: keep `@Data`. So the entities are not touched by
this task.
