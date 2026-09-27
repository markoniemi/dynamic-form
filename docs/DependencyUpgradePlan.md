# Dependency Upgrade Plan

**Generated:** 2026-09-27  
**Status:** Phase 1 Complete (2026-09-27)  
**Stack:** Spring Boot 4.0.3 (Java 21) | React 19.3.0 | TypeScript 7.0.2 candidate | Vitest 4.1.5  
**Tooling:** IDEA MCP (IDE-integrated) for all builds, tests, and linting

---

## How to Use This Plan

All commands use **IDEA MCP tools** instead of shell commands for better IDE integration:

| Task | Tool | Benefit |
|------|------|---------|
| Run npm/mvn commands | `mcp__idea__execute_terminal_command` | Full output, IDE context |
| Check compilation errors | `mcp__idea__get_file_problems` | IDE diagnostics, faster than build |
| Lint files | `mcp__idea__lint_files` | IDE inspections, better than CLI |
| Build project | `mcp__idea__build_project` | Structured diagnostics, error details |

See each phase below for specific IDEA MCP commands to run.

---

## Frontend Dependencies Status

### Current Versions
- **React:** 19.2.5 → 19.3.0 available (patch)
- **TypeScript:** 6.0.3 (current) → 7.0.2 available (major)
- **Vite:** 8.0.10 → 8.3.1 available (minor)
- **Vitest:** 4.1.5 (current) → 5.0.2 available (major)

### Patch Updates Available (Low Risk)
Safe to apply immediately via `npm update`:

| Package | Current | Latest | Risk |
|---------|---------|--------|------|
| @hookform/resolvers | 5.2.2 | 5.9.1 | Low |
| @supabase/supabase-js | 2.105.3 | 2.117.2 | Low |
| @tanstack/react-query | 5.100.9 | 5.104.0 | Low |
| @testing-library/dom | 10.4.1 | 10.4.2 | Low |
| @testing-library/react | 16.3.2 | 16.3.3 | Low |
| @testing-library/user-event | 14.6.1 | 14.6.7 | Low |
| @types/react | 19.2.14 | 19.3.0 | Low |
| @types/react-dom | 19.2.3 | 19.3.0 | Low |
| @vitejs/plugin-react | 6.0.1 | 6.1.1 | Low |
| eslint | 10.3.0 | 10.11.0 | Low |
| eslint-plugin-react-refresh | 0.5.2 | 0.5.7 | Low |
| globals | 17.6.0 | 17.12.0 | Low |
| i18next | 26.0.8 | 26.4.2 | Low |
| i18next-http-backend | 4.0.0 | 4.0.2 | Low |
| prettier | 3.8.3 | 3.9.9 | Low |
| react | 19.2.5 | 19.3.0 | Low |
| react-dom | 19.2.5 | 19.3.0 | Low |
| react-hook-form | 7.75.0 | 7.89.0 | Low |
| react-i18next | 17.0.6 | 17.0.15 | Low |
| react-router-dom | 7.15.0 | 7.18.4 | Low |
| typescript-eslint | 8.59.2 | 8.70.1 | Low |
| vite | 8.0.10 | 8.3.1 | Low |
| zod | 4.4.3 | 4.6.5 | Low |

**Action:** `npm update` — all tests should pass.

### Major Updates (Requires Testing)

| Package | Current | Latest | Notes | Priority |
|---------|---------|--------|-------|----------|
| TypeScript | 6.0.3 | 7.0.2 | New major. Review breaking changes in release notes. May require tsconfig updates. | Medium |
| Vitest | 4.1.5 | 5.0.2 | Major version. Check API compatibility. Likely needs @vitest/coverage-v8 5.0.2 too. | Medium |
| @vitest/coverage-v8 | 4.1.5 | 5.0.2 | Paired with vitest upgrade. | Medium |
| @testing-library/jest-dom | 6.9.1 | 7.0.1 | Major update. Breaking changes possible. | Medium |
| lucide-react | 1.14.0 | 1.48.0 | Major jump (34 versions). Verify icon compatibility. Likely safe. | Low |

**Action:** Test individually. Start with patch updates first.

---

## Backend Dependencies Status

### Spring Boot 4.0.3 (Current)
- Latest Spring Boot 4.x: 4.0.3 ✓ (no updates in 4.x line)
- Spring Boot 5.x planned for future
- **Status:** Current, stable. No action needed.

### Java
- **Current:** Java 21 (LTS)
- **Latest LTS:** Java 21 ✓
- **Status:** Current. Java 23 is available but not LTS.

### Backend Key Dependencies (via Spring Boot parent)
- **MapStruct:** 1.5.5.Final (current)
- **Testcontainers:** 2.0.3 (current)
- **PostgreSQL driver:** 42.7.2 (current)
- **Jackson YAML:** 2.18.1 (test scope)
- **Playwright:** 1.48.0 (test scope, manual version)

**Action:** Backend dependencies are current. Spring Boot 4.0.3 handles most transitive deps.

---

## Recommended Update Order

### Phase 1: Frontend Patch Updates (Safe, Low Risk)
**Priority:** Medium  
**Risk:** Low  
**Effort:** ~5 minutes

**Using IDEA MCP:**

1. **Install patch updates:** `mcp__idea__execute_terminal_command`
   ```
   command: "npm update"
   projectPath: "frontend"
   ```

2. **TypeScript compile check:** `mcp__idea__execute_terminal_command`
   ```
   command: "npm run compile"
   projectPath: "frontend"
   ```

3. **Build:** `mcp__idea__execute_terminal_command`
   ```
   command: "npm run build"
   projectPath: "frontend"
   ```

4. **Test:** `mcp__idea__execute_terminal_command`
   ```
   command: "npm test"
   projectPath: "frontend"
   ```

5. **Lint:** `mcp__idea__lint_files`
   ```
   files: ["frontend/src/**/*.ts", "frontend/src/**/*.tsx"]
   projectPath: "frontend"
   min_severity: "warning"
   ```

**Checklist:**
- [ ] All npm patches apply cleanly
- [ ] TypeScript compile passes (no errors from step 2)
- [ ] Build succeeds (no errors from step 3)
- [ ] All tests pass (no failures from step 4)
- [ ] Linting clean (no errors from step 5)

### Phase 2: Frontend Major Updates (Requires Testing)
**Priority:** Low-Medium  
**Risk:** Medium  
**Effort:** ~30 minutes per update

Do these one at a time. For each, use IDEA MCP tools:

**Common workflow for each update:**

1. **Install:** `mcp__idea__execute_terminal_command`
   ```
   command: "npm install"
   projectPath: "frontend"
   ```

2. **Check problems:** `mcp__idea__get_file_problems`
   ```
   filePath: "frontend/src/main.tsx"
   projectPath: "frontend"
   errorsOnly: true
   ```

3. **Compile:** `mcp__idea__execute_terminal_command`
   ```
   command: "npm run compile"
   projectPath: "frontend"
   ```

4. **Build:** `mcp__idea__execute_terminal_command`
   ```
   command: "npm run build"
   projectPath: "frontend"
   ```

5. **Test:** `mcp__idea__execute_terminal_command`
   ```
   command: "npm test"
   projectPath: "frontend"
   ```

6. **Lint:** `mcp__idea__lint_files`
   ```
   files: ["frontend/src/**/*.ts", "frontend/src/**/*.tsx"]
   projectPath: "frontend"
   min_severity: "warning"
   ```

**Updates to apply (one at a time):**

1. **TypeScript 6.0.3 → 7.0.2**
   - Review [TypeScript 7 release notes](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-7-0.html)
   - Check tsconfig.json for deprecated/removed options
   - Edit package.json, then run workflow above

2. **Vitest 4.1.5 → 5.0.2**
   - Must upgrade @vitest/coverage-v8 to 5.0.2 in same step
   - Review [Vitest v5 migration guide](https://vitest.dev/guide/migration.html)
   - Edit package.json (both packages), then run workflow above

3. **@testing-library/jest-dom 6.9.1 → 7.0.1**
   - Review [jest-dom v7 breaking changes](https://github.com/testing-library/jest-dom/releases/tag/v7.0.0)
   - Edit package.json, then run workflow above

4. **lucide-react 1.14.0 → 1.48.0**
   - Verify FileText icon (or icons used) still exists
   - Edit package.json, then run workflow above

### Phase 3: Backend (No Action Needed)
**Status:** Spring Boot 4.0.3 is current. No updates available.

---

## Testing Checklist

### After All Patch Updates

**Frontend validation via IDEA MCP:**

1. `mcp__idea__execute_terminal_command`
   ```
   command: "npm run compile"
   projectPath: "frontend"
   ```

2. `mcp__idea__execute_terminal_command`
   ```
   command: "npm run build"
   projectPath: "frontend"
   ```

3. `mcp__idea__execute_terminal_command`
   ```
   command: "npm test"
   projectPath: "frontend"
   ```

4. `mcp__idea__lint_files`
   ```
   files: ["frontend/src/**/*.ts", "frontend/src/**/*.tsx"]
   projectPath: "frontend"
   min_severity: "error"
   ```

**Full stack build via IDEA MCP:**

1. `mcp__idea__build_project`
   ```
   projectPath: "C:\Users\marko\Documents\Git\dynamic-form"
   rebuild: true
   timeout: 300000
   ```

### After Each Major Update
- Run full stack tests above
- Manual smoke test via IDE: Start dev server, verify key features work
- Check diagnostics: `mcp__idea__get_file_problems` on modified files

---

## Current Outdated Summary

**Patch updates waiting:** 24 frontend packages  
**Major updates available:** 5 frontend packages  
**Backend:** All current (Spring Boot 4.0.3, Java 21)  
**Last checked:** 2026-09-27

---

## Completion Status

### Phase 1: Complete ✓ (2026-09-27)
- [x] All 24 patch updates applied via `npm update`
- [x] TypeScript compile: passed
- [x] Build: passed (vite 8.3.1)
- [x] Tests: 47 passed, 44.31% coverage
- [x] Lint: passed (0 errors)

No code changes needed. All tests passing.

---

## Notes

- **IDEA MCP tools:** All commands use IDE-integrated tools for better diagnostics and error reporting
  - `execute_terminal_command` runs npm/mvn with full output
  - `lint_files` uses IDE inspections (better than CLI linting)
  - `get_file_problems` catches type errors before compilation
  - `build_project` provides structured build diagnostics
- Frontend patch updates should be applied first (quick win)
- TypeScript 7 and Vitest 5 are the riskiest updates due to major version jumps
- Spring Boot 4.0.3 is recent and stable; no pressure to upgrade
- testcontainers 2.0.3 works well with Spring Boot 4.x
- Always run full test suite after major dependency updates
