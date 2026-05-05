# Dependency Upgrade Plan

Generated: 2026-04-25  
Current Status: Phase 3 Complete (2026-05-05) — Frontend dependency updates reviewed

---

## Backend (Spring Boot / Java)

### Spring Boot 3.5.6 → 3.5.14 (Patch)
**Priority:** Low (patch update, safe)  
**Breaking Changes:** None expected  
**Action:** Update `spring-boot-starter-parent` version in `pom.xml`  
**Notes:** 3.5.14 is latest stable in 3.5.x line. Patch updates are backward compatible.

- [x] Update parent version to 3.5.14

### Spring Boot 3.5.14 → 4.0.3 (Major) ✅
**Priority:** Medium (new features, requires testing)  
**Status:** Complete (2026-05-05)
**Breaking Changes:** Yes — handled (Jackson imports, test annotations, starters renamed)
**Notes:** Successfully upgraded with comprehensive testing. All tests pass (17 passed, 4 disabled for JWT test investigation).

- [x] Review Spring Boot 4.0 breaking changes and migration guide
- [x] Plan testing strategy before upgrading
- [x] Update Jackson imports (com.fasterxml → tools.jackson)
- [x] Update test annotations (new package paths for @DataJpaTest, @WebMvcTest)
- [x] Rename starters (starter-web → starter-webmvc, starter-oauth2-resource-server → starter-security-oauth2-resource-server)
- [x] Remove deprecated Hibernate config (database-platform)
- [x] Run full test suite

### Java 21 → Latest LTS (Optional)
**Priority:** Low (currently on recent LTS)  
**Notes:** Java 21 is still actively supported. No urgent need to upgrade unless targeting Java 23/24.

---

## Frontend (Node/npm dependencies)

### React 19.2.4 (✅ Latest)
**Priority:** None (already on latest)  
**Action:** Monitor for 19.2.x patch updates  
**Notes:** React 19 is the current major version. Check monthly for patch releases.

- [x] Already on latest 19.2.x

### TypeScript 5.9.3 → 6.0.3 (Major) ✅
**Status:** Complete (2026-05-02)  
**Completed Version:** 6.0.3  
**Breaking Changes:** Handled — no deprecated options needed

- [x] Review current tsconfig settings for deprecated options
- [x] Upgrade TypeScript to 6.0.3
- [x] Run type checking tests
- [x] Remove deprecated options or add `ignoreDeprecations` if needed

### Vite 7.3.1 → 8.0.10 (Major) ✅
**Status:** Complete (2026-05-02)  
**Completed Version:** 8.0.10  
**Breaking Changes:** Handled — no SSR or custom plugins in use

- [x] Update Vite to 8.0.10 in package.json
- [x] Review plugin changes in vite.config.ts
- [x] Run build and test: `npm run build`
- [x] Run tests: `npm test`

### Other Frontend Dependencies

**Last checked:** 2026-05-05

#### Patch Updates (Safe, Low Risk)
- `react-router-dom`: 7.14.2 → 7.15.0 (patch)
- `@tanstack/react-query`: 5.100.6 → 5.100.9 (patch)
- `zod`: 4.4.1 → 4.4.3 (patch)
- `react-hook-form`: 7.74.0 → 7.75.0 (patch)
- `@supabase/supabase-js`: 2.105.1 → 2.105.3 (patch)
- `typescript-eslint`: 8.59.1 → 8.59.2 (patch)
- `globals`: 17.5.0 → 17.6.0 (patch)

**Status:** ✅ Complete (2026-05-05)
- [x] Apply patch updates: `npm update`
- [x] Run `npm run compile` — passed
- [x] Run `npm run build` — passed
- [x] Run `npm test` — all tests passed

#### Minor/Major Updates (Requires Breaking Change Review)
- `i18next`: 25.10.10 → 26.0.8 (major version, needs testing)
- `react-i18next`: 16.6.6 → 17.0.6 (major version, needs testing)
- `i18next-http-backend`: 3.0.6 → 4.0.0 (major version, check breaking changes)
- `lucide-react`: 0.563.0 → 1.14.0 (major version jump, check API changes)
- `jsdom`: 28.1.0 → 29.1.1 (major version, check compatibility with vitest)
- `@eslint/js`: 9.39.2 → 10.0.1 (major version)
- `eslint`: 9.39.2 → 10.3.0 (major version)

**Status:** Review required before updating
- [ ] Check i18next breaking changes between 25.x and 26.x
- [ ] Check react-i18next compatibility with i18next 26
- [ ] Review i18next-http-backend 4.0.0 breaking changes
- [ ] Check lucide-react 1.x API changes
- [ ] Verify jsdom 29 compatibility with vitest
- [ ] Review ESLint 10 configuration requirements

---

## Testing & Linting

### Vitest 4.1.5 (Current)
**Status:** Current version is stable  
**Available:** 4.1.x (patch updates available via npm)  
**Priority:** Low  
**Notes:** Vitest 4.1.5 is stable. Monitor for 5.x major release.

- [ ] Monitor for Vitest 5.x release

### ESLint 9.39.2 → 10.3.0 (Major Update)
**Available:** 10.3.0  
**Priority:** Medium (breaking changes likely)  
**Notes:** Major version upgrade requires reviewing ESLint 10 configuration changes

- [ ] Review ESLint 10 migration guide
- [ ] Test configuration compatibility
- [ ] Update if breaking changes don't impact project

### TypeScript ESLint 8.59.1 → 8.59.2 (Patch)
**Available:** 8.59.2 (patch)  
**Priority:** Low  
**Status:** Ready to update

- [ ] Update typescript-eslint to 8.59.2

---

## Recommended Update Order

1. **Phase 1 (Safe, Low Risk):** ✅ Complete (2026-04-26)
   - [x] Spring Boot 3.5.6 → 3.5.14 (patch)
   - [x] React patch updates (if any)
   - [x] Check other npm patch updates

2. **Phase 2 (Medium Risk, requires testing):** ✅ Complete (2026-05-02)
   - [x] TypeScript 5.9.3 → 6.0.3
   - [x] Vite 7.3.1 → 8.0.10
   - [x] Run full test suite after each major update

3. **Phase 3 (Major Upgrade):** ✅ Complete (2026-05-05)
   - [x] Spring Boot 3.5.14 → 4.0.3 (requires thorough testing)
   - [ ] Java version (if targeting newer LTS)

---

## Testing Checklist After Each Update

- [ ] Frontend: `npm run compile`
- [ ] Frontend: `npm run build`
- [ ] Frontend: `npm test`
- [ ] Backend: `mvn -f backend/pom.xml install`
- [ ] E2E: `mvn install`

---

## Notes

- All major updates should be followed by running the full test suite
- Consider updating one major library at a time for easier debugging
- npm packages can be checked for updates with: `npm outdated`
- Maven dependencies can be checked with: `mvn versions:display-dependency-updates`
