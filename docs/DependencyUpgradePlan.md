# Dependency Upgrade Plan

Generated: 2026-04-25  
Current Status: Not started

---

## Backend (Spring Boot / Java)

### Spring Boot 3.5.6 → 3.5.14 (Patch)
**Priority:** Low (patch update, safe)  
**Breaking Changes:** None expected  
**Action:** Update `spring-boot-starter-parent` version in `pom.xml`  
**Notes:** 3.5.14 is latest stable in 3.5.x line. Patch updates are backward compatible.

- [ ] Update parent version to 3.5.14

### Spring Boot 3.5.x → 4.0.3 (Major)
**Priority:** Medium (new features, requires testing)  
**Breaking Changes:** Yes — review Spring Boot 4.0 migration guide  
**Action:** Plan separate from patch upgrades; requires comprehensive testing  
**Notes:** 4.0.3 is latest in 4.0.x line. Major version upgrade should be done carefully.

- [ ] Review Spring Boot 4.0 breaking changes and migration guide
- [ ] Plan testing strategy before upgrading

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

### TypeScript 5.9.3 → 6.0.2 (Major)
**Priority:** Medium (potential type safety improvements)  
**Breaking Changes:** Yes — deprecations for ES5 target, `outFile`, `moduleResolution: classic`  
**Action:** Requires tsconfig.json updates and testing  
**Migration Path:**
1. Review deprecated options in your tsconfig.app.json
2. Upgrade to TypeScript 6.0.2
3. Test type checking with `npm run compile`
4. Add `ignoreDeprecations: "6.0"` if needed as temporary workaround

- [ ] Review current tsconfig settings for deprecated options
- [ ] Upgrade TypeScript to 6.0.2
- [ ] Run type checking tests
- [ ] Remove deprecated options or add `ignoreDeprecations` if needed

### Vite 7.3.1 → 8.0.7 (Major)
**Priority:** Medium (new build features, requires testing)  
**Breaking Changes:** Yes — SSR API changes, plugin hook changes  
**Action:** Update and test build pipeline  
**Migration Notes:**
- If using SSR: migrate `options.ssr` to `this.environment.config.consumer === 'server'`
- Review Vite plugin documentation if using custom plugins
- Test full build: `npm run build`

- [ ] Update Vite to 8.0.7 in package.json
- [ ] Review plugin changes in vite.config.ts
- [ ] Run build and test: `npm run build`
- [ ] Run tests: `npm test`

### Other Frontend Dependencies

#### React Router 7.13.0 (Check for updates)
**Priority:** Low-Medium  
**Action:** Check npm for latest 7.x patch updates

- [ ] Check latest 7.x version and update if needed

#### @tanstack/react-query 5.90.20 (Check for updates)
**Priority:** Low  
**Action:** Check npm for latest 5.x patch updates

- [ ] Check latest 5.x version and update if needed

#### Zod 4.3.6 (Check for updates)
**Priority:** Low  
**Action:** Check npm for latest 4.x patch updates

- [ ] Check latest 4.x version and update if needed

#### Bootstrap 5.3.8 (Check for updates)
**Priority:** Low  
**Action:** Check npm for latest 5.3.x patch updates

- [ ] Check latest 5.3.x version and update if needed

---

## Testing & Linting

### Vitest 4.0.18 (Check for updates)
**Priority:** Low  
**Action:** Check for latest 4.x or 5.x versions

- [ ] Check latest version and update if needed

### ESLint & TypeScript ESLint
**Priority:** Low-Medium  
**Notes:** Current setup uses ESLint 9.39.2 and typescript-eslint 8.54.0  
**Action:** Check for compatibility and latest versions

- [ ] Verify compatibility between ESLint and typescript-eslint versions
- [ ] Update if needed

---

## Recommended Update Order

1. **Phase 1 (Safe, Low Risk):**
   - [ ] Spring Boot 3.5.6 → 3.5.14 (patch)
   - [ ] React patch updates (if any)
   - [ ] Check other npm patch updates

2. **Phase 2 (Medium Risk, requires testing):**
   - [ ] TypeScript 5.9.3 → 6.0.2
   - [ ] Vite 7.3.1 → 8.0.7
   - [ ] Run full test suite after each major update

3. **Phase 3 (Future, when ready):**
   - [ ] Spring Boot 3.5.x → 4.0.3 (requires thorough testing)
   - [ ] Java version (if targeting newer LTS)

---

## Testing Checklist After Each Update

- [ ] Backend: `mvn clean test`
- [ ] Frontend: `npm run compile`
- [ ] Frontend: `npm run build`
- [ ] Frontend: `npm test`
- [ ] E2E: `mvn verify` (if applicable)
- [ ] Manual browser testing of key features

---

## Notes

- All major updates should be followed by running the full test suite
- Consider updating one major library at a time for easier debugging
- npm packages can be checked for updates with: `npm outdated`
- Maven dependencies can be checked with: `mvn versions:display-dependency-updates`
