# Claude Code Automation Recommendations

## Codebase Profile

- **Type**: Full-stack monolith — Java 21 / Spring Boot 3.5 backend + React 19 / TypeScript frontend
- **Framework**: Spring Boot (REST, JPA, Security/OAuth2) + Vite + React + react-hook-form + Zod
- **Key Libraries**: Supabase, TanStack Query, react-router-dom, Vitest, JUnit 5 + Testcontainers
- **Already configured**: Playwright MCP, Stop hook (compile check), `/test` command, Playwright permissions

---

## MCP Servers

### context7 — Live library documentation

**Why**: You use many actively-changing libraries (React 19, Spring Boot 3.5, TanStack Query v5, Zod v4, react-hook-form v7). Context7 fetches up-to-date docs so Claude doesn't hallucinate outdated APIs.

**Install**:
```bash
claude mcp add context7
```
Then check it into `.mcp.json` so the whole team benefits.

### GitHub MCP — Issues, PRs, Actions

**Why**: You have a `.github/` directory with Copilot instructions and likely CI workflows. GitHub MCP lets Claude read/create issues and PRs directly — useful for linking code reviews to issues.

**Install**:
```bash
claude mcp add github -- -e GITHUB_PERSONAL_ACCESS_TOKEN=<your_token>
```

---

## Hooks

### PostToolUse: Auto-format on frontend file edits

**Why**: Prettier is configured (`prettier: 3.8.1` in devDependencies) but there's no hook to run it automatically. Without this, Claude's edits can drift from the project's code style.

**Add to `.claude/settings.json`** under `"hooks"`:
```json
"PostToolUse": [
  {
    "matcher": "Edit|Write",
    "hooks": [
      {
        "type": "command",
        "command": "bash -c 'echo \"$CLAUDE_TOOL_INPUT\" | python3 -c \"import sys,json; f=json.load(sys.stdin).get(\\\"file_path\\\",\\\"\\\"); print(f)\" | grep -E \"frontend/.*\\.(ts|tsx|js|jsx|json|css)$\" && cd frontend && npx prettier --write \"$(echo \"$CLAUDE_TOOL_INPUT\" | python3 -c \"import sys,json; f=json.load(sys.stdin).get(\\\"file_path\\\",\\\"\\\"); print(f.replace(\\\"$(pwd)/frontend/\\\",\\\"\\\"))\" 2>/dev/null)\" 2>/dev/null || true'",
        "statusMessage": "Formatting with Prettier..."
      }
    ]
  }
]
```

### PreToolUse: Block `.env` / secret file edits

**Why**: Your app uses OAuth2 and Supabase — both rely on credentials typically stored in `.env` or `application-*.properties` files. A guard prevents accidental leaks.

**Add to `.claude/settings.json`** under `"hooks"`:
```json
"PreToolUse": [
  {
    "matcher": "Edit|Write",
    "hooks": [
      {
        "type": "command",
        "command": "bash -c 'echo \"$CLAUDE_TOOL_INPUT\" | python3 -c \"import sys,json; f=json.load(sys.stdin).get(\\\"file_path\\\",\\\"\\\"); exit(1 if any(x in f for x in [\\\".env\\\",\\\"application-local\\\",\\\"secrets\\\"]) else 0)\"'",
        "statusMessage": "Checking file safety..."
      }
    ]
  }
]
```

---

## Skills

### `/gen-test` — Generate tests matching project patterns

**Why**: Your project has specific test naming conventions (`MethodUnderTestWithStateExpectedBehavior`), uses Mockito + JUnit5 for backend and React Testing Library + Vitest for frontend. A skill that embeds these rules produces correct tests immediately.

**Create `.claude/skills/gen-test/SKILL.md`**:
```markdown
---
name: gen-test
description: Generate unit tests following project conventions (JUnit5+Mockito for backend, RTL+Vitest for frontend)
---

Generate tests for the file or component the user specifies.

Backend rules:
- Use JUnit 5 assertions, Mockito for mocks, constructor injection
- Naming: `methodName` / `methodNameWithStateExpectedBehavior`
- No integration tests (no @SpringBootTest, no DB)

Frontend rules:
- Use React Testing Library + Vitest
- Test component behavior, not implementation
- Use `userEvent` for interactions

Read the existing tests in the same package/folder first to match style.
```

### `/pr-check` — Pre-PR checklist

**Why**: You have detailed coding conventions (Google Java Style, Airbnb TS, no `any`, constructor injection, etc.). A skill that runs through the checklist before opening a PR catches style drift early.

**Create `.claude/skills/pr-check/SKILL.md`**:
```markdown
---
name: pr-check
description: Review staged/uncommitted changes against project coding conventions before opening a PR
---

Review all changed files against the project's conventions in .github/copilot-instructions.md:

1. Java: Google Java Style, Lombok used, constructor injection, no ResponseStatusException in services
2. TypeScript: no `any`, PascalCase components, useQuery for API calls, Zod+react-hook-form for validation
3. Tests present for new logic
4. No .env or secrets committed

Report violations grouped by file. If clean, confirm with one line.
```

---

## Subagents

### `security-reviewer` — OAuth2/Supabase security audit

**Why**: Your app has Spring Security with OAuth2, Supabase client-side keys, and `@PreAuthorize` annotations. A dedicated security subagent can scan for misconfigured CORS, exposed endpoints, or improperly scoped tokens when you ask for a security review.

**Create `.claude/agents/security-reviewer.md`**:
```markdown
---
name: security-reviewer
description: Reviews security-sensitive code — OAuth2 config, @PreAuthorize annotations, Supabase key exposure, CORS settings
---

You are a security reviewer for a Spring Boot + React OAuth2 application.

When invoked, scan for:
1. Endpoints missing @PreAuthorize or Spring Security rules
2. CORS config that's too permissive
3. Supabase keys exposed in frontend bundle (should use env vars)
4. Sensitive data leaking through DTOs or API responses
5. JWT/token validation gaps

Report findings with file:line references and severity (HIGH/MEDIUM/LOW).
```

---

## What's Already Well Configured

- **Playwright MCP** — browser automation for UI testing
- **Stop hook** — compiles both frontend (tsc) and backend (mvn) after each session
- **`/test` command** — runs Vitest + JUnit and summarizes results
- **Accepted edits mode** — good for a trusted dev workflow
