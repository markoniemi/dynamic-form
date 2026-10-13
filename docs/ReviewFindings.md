# Review Findings

Ponytail audit of the whole repo, 2026-10-13. Read-only: nothing was built or run.

**What this repo does:** Dynamic Form is a Spring Boot and React app. Admins define forms as JSON. Logged-in users fill them in and edit their own submissions. It deploys to AWS (ECS, RDS, ALB) and uses an external OAuth2 server for login. Load assumed: small internal tool, tens of users, one or two ECS tasks, a few thousand submissions.

## Must fix

### 1. Admin role never works with real tokens (`SecurityConfig.java:25-50`, `SecurityUtils.java:21-28`)
- **What this is:** Admin actions (create, edit and delete forms, delete submissions, see all submissions) check for the authority `ROLE_ADMIN`.
- **Problem:** Nothing turns a token claim into `ROLE_ADMIN`. Spring's default only reads `scope` and gives `SCOPE_x`. The sibling `oauth2-server` only passes roles to its user store. No token customizer found there. Cognito puts groups in `cognito:groups`, which is not read either. Tests fake the authority in `TestUsers`, so they pass. In a real run every admin gets 403 on writes and sees only their own submissions. Read from code, not run against the live server.
- **Fix:** Add a `JwtAuthenticationConverter` bean that maps the roles/groups claim to `ROLE_*`. The auth server must also put that claim in the access token. Add one integration test with a real token.
- **If we skip it:** Admin features are dead outside tests.

### 2. Admin cannot delete other people's submissions (`FormDataService.java:75-83`)
- **What this is:** Only admins may call delete.
- **Problem:** The service then requires `submittedBy == username`. An admin can delete only their own rows. Unit tests lock this in (`FormDataServiceTest:111`).
- **Fix:** Pass an `isAdmin` flag, or skip the owner check in the delete path (endpoint is already admin-only).
- **If we skip it:** Admins cannot remove spam or bad data.

### 3. Submissions are never checked against the form (`FormDataController.java:34`, `FormDataService.java:24`)
- **What this is:** `POST /api/form-data/{key}` stores whatever JSON it receives.
- **Problem:** Required fields, types, option values and unknown keys are only checked in the browser. `curl` can store an empty body, a 50 MB blob, or `{"rating":"banana"}`. `@Valid` on `FormData` checks nothing, because the entity has no constraints. Same gap on `PUT`.
- **Fix:** In the service, load the form and reject unknown names, missing required fields and bad select/radio values. Cap body size (a filter or ALB limit; `max-http-form-post-size` does not cover JSON).
- **If we skip it:** Garbage rows and storage abuse. Any logged-in user can fill the database.

### 4. Startup seed can crash with more than one task (`DatabaseInitializer.java:34-45`)
- **What this is:** On every start, each form JSON file is checked with `existsByFormKey` and then saved.
- **Problem:** Check-then-write race. Two tasks starting together (desired count above 1, or rolling deploy overlap) both see "missing". One hits the unique key and fails to start. Also, a seed form an admin deleted comes back on the next restart.
- **Fix:** Catch `DataIntegrityViolationException` per form and continue. Or insert seeds in a Flyway migration with `ON CONFLICT DO NOTHING`, which also fixes the "deleted form comes back" problem.
- **If we skip it:** Intermittent boot failure during deploys.

### 5. Database has no safety net (`terraform/rds.tf:14-31`, `main.tf:1-12`)
- **What this is:** The RDS instance holds all the data.
- **Problem:** `skip_final_snapshot = true`, no `deletion_protection`, no `storage_encrypted`, no `backup_retention_period`, no Multi-AZ. No remote state backend, so `terraform.tfstate` (contains `db_password`) sits on one laptop. One `terraform destroy` or `cleanup.sh --force` deletes all data with no snapshot.
- **Fix:** In `aws_db_instance` set `storage_encrypted = true`, `deletion_protection = true`, `backup_retention_period = 7`, `skip_final_snapshot = false`. Add an S3 backend with a lock table.
- **If we skip it:** Unrecoverable data loss on a mistake. Lost laptop means lost state.

### 6. Private TLS key and cert are in git (`.certificates/dynamic-form-localhost.key`)
- **What this is:** A self-signed localhost cert pair, plus `.arn` and `openssl.conf`.
- **Problem:** The private key is tracked and stays in history. Anyone with the repo can impersonate that cert. If `certificate.sh` imported it into ACM (the `.arn` file suggests so), that ACM cert is compromised. The ALB uses a different, hard-coded ARN.
- **Fix:** `git rm --cached .certificates/*.key`, add `.certificates/` to `.gitignore`, regenerate the cert, revoke or delete the ACM entry.
- **If we skip it:** A leaked key, even if only for a dev domain.

## Should fix

### 7. No test covers real JWT to role mapping (`backend/src/test`)
- **What this is:** All controller tests inject a fake `JwtAuthenticationToken` with hand-made authorities (`TestUsers.java:45`).
- **Problem:** That is why finding 1 went unnoticed. The Playwright test logs in as a real user, but no admin-only action was seen asserted.
- **Fix:** One integration test using `OAuth2Container` that logs in as `admin` and calls `POST /api/forms`.
- **If we skip it:** Auth config regressions pass CI.

### 8. Deleting or editing a form orphans submissions (`FormService.java:44-62`, `V1__initial_schema.sql:11`)
- **What this is:** `form_data.form_key` is a plain string with no foreign key.
- **Problem:** Deleting a form leaves its submissions; opening one gives a 404 on the form. Editing fields makes old data not match the new form.
- **Fix:** Add a foreign key (or block delete when submissions exist). Decide a rule for edits, such as warn if submissions exist.
- **If we skip it:** Broken pages and dead data.

### 9. Create-form race returns 500 instead of 409 (`FormController.java:44-47`)
- **What this is:** The controller calls `exists()` and then `save()`.
- **Problem:** Two simultaneous creates both pass the check. The second gets `DataIntegrityViolationException`, which falls into the generic 500 handler.
- **Fix:** Remove the pre-check. Map `DataIntegrityViolationException` to 409 in `GlobalExceptionHandler`.
- **If we skip it:** Confusing 500s. Rare, but a common pattern.

### 10. Form validation is too loose (`FieldDto.java`, `FormDto.java`)
- **What this is:** The create/update form API takes field definitions.
- **Problem:** `select`/`radio` fields can have no options. Two fields can share a name; the second overwrites the first in the data map and in React's `key`. Options can repeat.
- **Fix:** Add a class-level check for unique field names, and for non-empty options when the type is select or radio.
- **If we skip it:** Admins can build forms that cannot render or submit correctly.

### 11. List endpoints have no limit and one query is missing an index (`FormDataController.java:52-58`, `V1__initial_schema.sql:21-22`)
- **What this is:** `GET /api/form-data` returns all rows. Non-admins query by `submitted_by`.
- **Problem:** No pagination. No index on `submitted_by`, only on `form_key` and `submitted_at`. `FormService.getAll` also loads the full JSONB `fields` of every form just to return key and title.
- **Fix:** Add `CREATE INDEX ... ON form_data(submitted_by, submitted_at DESC)` in a new migration. Add a page size (Spring Data `Pageable`). Return a key/title projection for the form list.
- **If we skip it:** Fine at hundreds of rows, slow at hundreds of thousands.

### 12. Broad IAM and old TLS policy (`terraform/iam.tf`, `alb.tf:42`)
- **What this is:** The ECS roles and ALB listener.
- **Problem:** Both roles can read every SSM parameter in the account (`parameter/*`). The task role does not need any of it, because ECS injects secrets using the execution role. The ALB uses `ELBSecurityPolicy-TLS-1-2-2017-01` (old). The ACM certificate ARN with the account ID is hard-coded. Image is `:latest`, so no rollback by tag.
- **Fix:** Limit the execution role to `parameter/rds-db-*` and `parameter/oauth2-issuer-uri`. Delete `ecs_task_parameter_store_policy` (and update `depends_on` in `ecs.tf`). Use `ELBSecurityPolicy-TLS13-1-2-2021-06`. Move the ARN to a variable. Tag images with the git SHA.
- **If we skip it:** Bigger blast radius if the container is compromised.

### 13. Weak demo users in the real Cognito pool (`setup.sh:272-327`, `terraform/variables.tf:84`)
- **What this is:** `setup.sh` creates `admin/admin` and `user/user` in the deployed Cognito pool. `create_test_users` defaults to `true`.
- **Problem:** If creation works, anyone on the internet who guesses `admin/admin` can log in to the public ALB. Pool password policy is the minimum (8 chars, no complexity).
- **Fix:** Default `create_test_users` to `false`. Generate random passwords. Never ship `admin/admin` to a public URL.
- **If we skip it:** A public login with a default password.

### 14. AWS admin keys stored in GitHub for no workflow (`setup.sh:343-372`, `.github/workflows/maven-build.yaml`)
- **What this is:** `setup.sh` creates an IAM user and key and puts them in GitHub secrets.
- **Problem:** The only workflow (`maven-build.yaml`) never uses any secret. The access key sits there unused, and is printed to the terminal.
- **Fix:** Remove the IAM user, the key and the GitHub secrets steps until a deploy workflow exists. Then use GitHub OIDC instead of a long-lived key.
- **If we skip it:** A long-lived credential nobody uses.

## Nice to have

### 15. Delete unused dependencies (`frontend/package.json`, `backend/pom.xml`)
- **What this is:** `@supabase/supabase-js`, `zod` and `@hookform/resolvers` have no import in `frontend/src` or `frontend/test`. `spring-boot-starter-webflux` has no `WebClient` or reactive use in `backend/src`.
- **Problem:** Each is code to patch and download. WebFlux also pulls in Reactor and Netty beside Tomcat.
- **Fix:** Remove all four.
- **If we skip it:** More attack surface and slower builds.

### 16. Source maps ship in production (`vite.config.ts:25`)
- **What this is:** `sourcemap: true` goes into the jar, served from `/assets`.
- **Problem:** Anyone can read the original source.
- **Fix:** Set it to `false` or `'hidden'`.
- **If we skip it:** Source exposure. Low risk, source is not secret here.

### 17. Bootstrap CSS imported twice (`main.tsx:6`, `App.tsx:3`)
- **What this is:** Both files import `bootstrap.min.css`.
- **Fix:** Delete one line.

### 18. Dead code (`terraform/cognito-users.tf`, `FormDataMapper.toEntity`)
- **What this is:** The Terraform file's own comments say its resources are unused; the pool is made with the CLI. `FormDataMapper.toEntity` has no caller in `src/main`.
- **Fix:** Delete both (keep `cognito_user_pool_id` only if something else uses it).

### 19. Floating tool versions (`.mcp.json`, `docker-compose.yaml`)
- **What this is:** `@playwright/mcp@latest` and `oauth2-server:latest`.
- **Fix:** Pin both. A bad upstream release otherwise breaks the build or dev environment with no change in this repo.

About 3 smaller items left out, such as the `/h2-console/**` permit in `SecurityConfig` (harmless with no H2 in prod) and `ddl-auto: update` in the dev profile.

## Summary

**Verdict:** Fix 1 first. Admin access probably does not work on a real deploy, and the tests hide it. Then 2, 3 and 5. Rotate the key in 6 now.

**Lean:** -about 80 lines and -4 dependencies possible (15, 17, 18, plus IAM policy and GitHub secrets code in 12 and 14).

**Not checked:**
- `debt-bot/` Python, beyond skimming the git and API calls
- `deploy.sh`, `cleanup.sh` and `certificate.sh` beyond grep
- The `org.example:interface-log` library, which logs method calls. Not checked whether it writes submission payloads or tokens to logs.
- Most frontend pages (`EditForm`, `FormSubmission`)
- Frontend test quality
- Anything at runtime. Nothing was built or run, and no tests were run.
