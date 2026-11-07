# Ponytail repo audit — dynamic-form

Date: 2026-11-07. Whole-repo quality audit (`/ponytail:ponytail-audit`). This is a
report only; no code was changed.

**What this repo does:** It lets an admin define forms (a title plus a list of
fields) and lets logged-in users fill those forms in and submit them. Form
definitions and submissions are stored in PostgreSQL; a React page is served by
the same Spring Boot application; logging in goes through OAuth2 (a separate auth
server locally, AWS Cognito when deployed).

**Load assumed: small.** One ECS task (`ecs_desired_count` default `1`), a
`db.t3.micro` database with 10 GB of storage, and two demo users. So: tens of
users, hundreds to thousands of submissions. Findings are judged against that,
and where something only breaks at a larger size, the finding says so.

---

## Must fix

### 1. A deployed system gets an admin account with the password `admin`

(`setup.sh:270-332`, `terraform/outputs.tf:76-93`, `terraform/variables.tf:84-88`)

- **What this is:** `setup.sh` is the one-time AWS setup script. Among other
  things it creates login accounts in Cognito, the service that handles logging in.
- **Problem:** It creates a real user `admin` with the permanent password `admin`
  and puts it in the `ROLE_ADMIN` group. That group name is read by
  `RoleClaimsConverter` and becomes the `ROLE_ADMIN` permission, which every
  admin-only endpoint checks — creating and deleting forms, reading and deleting
  *all* users' submissions. Nothing gates this: `setup_cognito` runs
  unconditionally in `main()`. Separately, `terraform/outputs.tf` prints both
  passwords as a Terraform output with `sensitive = false`, so they appear in
  console output, in CI logs and in the Terraform state file; and
  `create_test_users` defaults to `true`.
- **Fix:** Delete the `test_users` output and the `locals.test_users` block
  (`terraform/cognito-users.tf:10-25`) — that block is already dead, as the
  comment at line 40 says the users are made by the CLI instead. In `setup.sh`,
  either generate a random password and print it once, or put the demo-user step
  behind an explicit flag that defaults to off.
- **If we skip it:** Anyone who finds the deployed URL can log in as a full
  administrator by guessing the most obvious password there is, and read every
  submission in the database.

### 2. The backend never checks a submission against the form it belongs to

(`backend/.../FormDataController.java:31-40`, `backend/.../FormDataService.java:29-38`)

- **What this is:** A form definition says what fields exist, what type each is,
  and which ones are `required` (`backend/.../entity/Field.java`).
- **Problem:** The submit endpoint takes `Map<String, Object> data` — any JSON
  object at all — and stores it. `create` only checks that the *form* exists. The
  `required`, `type` and `options` values on `Field` are never read anywhere in
  the backend's main code: grepping for them finds only `getFields`/`setFields`,
  used for saving and loading. All validation lives in the React page. Someone
  posting straight to the API with a valid token can store `{}`, miss every
  required field, send a string where a number belongs, or add fields the form
  does not have.
- **Fix:** In `FormDataService.create` (and `update`), loop over the form's
  fields: reject when a `required` field is missing or blank, and reject keys that
  no field declares. That is one loop in the place that already fetches the form.
- **If we skip it:** The submissions table fills with records that no form can
  display correctly, and the admin screens show blanks or crash on data they did
  not expect. Nothing detects it, because nothing validated it.

### 3. The production database can be destroyed with no way back

(`terraform/rds.tf:13-31`)

- **What this is:** The Terraform definition of the PostgreSQL database that holds
  every form and every submission.
- **Problem:** Three safety settings are missing or off. `skip_final_snapshot =
  true` means deleting or replacing the instance takes no last backup.
  `backup_retention_period` is not set, so automatic daily backups are not
  enabled. `storage_encrypted` is not set, so the disk is unencrypted.
  `deletion_protection` is not set, so nothing stops a delete. Several ordinary
  Terraform edits — changing the identifier, the engine version, the subnet group
  — replace the instance rather than updating it.
- **Fix:** Set `backup_retention_period = 7`, `storage_encrypted = true`,
  `deletion_protection = true`, and `skip_final_snapshot = false`. Four lines.
  Encryption cannot be turned on in place, so do it before there is real data.
- **If we skip it:** One `terraform apply` that happens to replace the instance
  loses every submission permanently, with no snapshot and no backup to restore.

---

## Should fix

### 4. An admin can read someone else's submission but not correct it

(`FormDataController.java:65-76` vs `FormDataService.java:42-52`)

- **What this is:** The same ownership rule, written in two places.
- **Problem:** The two disagree. `getSubmissionById` lets the request through if
  the caller owns the submission **or** is an admin. `FormDataService.update`
  throws unless `submittedBy` equals the caller — admins included. Delete is
  admin-only. So an admin sees a submission with a typo in it and gets 403 when
  trying to fix it, while being allowed to delete it outright.
- **Fix:** Decide the rule once and put it in one place. If admins may edit, pass
  the admin flag into `update` the way the controller already does for reads; if
  not, say so in the API and stop admins from deleting too.
- **If we skip it:** Confusing API behaviour that will be reported as a bug, and
  two copies of a security rule that will drift further apart.

### 5. Listing submissions loads the whole table into memory

(`FormDataService.java:62-64`, `FormDataRepository.java:12`)

- **What this is:** The admin submissions screen calls `getAll()`.
- **Problem:** `findAllByOrderBySubmittedAtDesc()` has no limit and no paging.
  Every submission is loaded as an object, mapped to a DTO, and serialised into
  one JSON response. Each submission also carries a free-form JSON blob of
  unbounded size (see finding 7).
- **Fix:** Take a `Pageable` and return a page. Spring Data needs only the method
  signature changed; the screen gains a page size.
- **If we skip it:** Fine at hundreds of rows. At the point where submissions
  reach tens of thousands, the admin screen becomes slow and then runs the task
  out of memory — and the only way back is a code change.

### 6. The per-user submission list has no index to use

(`backend/src/main/resources/db/migration/V1__initial_schema.sql:21-23`)

- **What this is:** An index lets the database find matching rows without reading
  the whole table.
- **Problem:** The migration creates indexes on `form_key` and `submitted_at`, but
  the query users actually run — `findBySubmittedByOrderBySubmittedAtDesc` —
  filters on `submitted_by`, which has none. Every user opening their own list
  causes a full scan of all submissions by all users. `@Index` appears nowhere in
  the entities either.
- **Fix:** One migration: `CREATE INDEX idx_form_data_submitted_by ON
  form_data(submitted_by, submitted_at DESC);` — it serves the filter and the sort
  together.
- **If we skip it:** Each user's list gets slower as *other* people's submissions
  accumulate, which is confusing to diagnose because the user's own data is small.

### 7. A submission can be any size

(`FormDataController.java:34-40`, `application.yaml`)

- **What this is:** The submitted JSON is stored in a `JSONB` column as-is.
- **Problem:** No limit is set anywhere — not on the request body, not on the map,
  not on the column. Spring's form-post limit does not apply to a JSON body. One
  authenticated caller can post a multi-megabyte object; it is parsed into memory,
  stored, and then loaded again by finding 5's unbounded list query.
- **Fix:** Set `server.tomcat.max-http-request-size` (or `max-swallow-size`) in
  `application.yaml`, and reject a submission over a sensible field count or size
  in `create`.
- **If we skip it:** Any logged-in user can bloat the database and exhaust the
  task's 2 GB of memory without doing anything that looks like an attack.

### 8. Two instances starting on an empty database collide

(`DatabaseInitializer.java:36-47`)

- **What this is:** On startup this seeds form definitions from JSON files,
  skipping ones that already exist.
- **Problem:** It checks `existsByFormKey` and then saves — two separate steps. If
  two tasks start at the same moment against a fresh database, both see "not
  there" and both insert. `form_key` is unique, so one insert fails, the exception
  escapes `run`, and that task fails to start.
- **Fix:** Catch the duplicate-key exception around the save and treat it as
  "already seeded", which is what the skip branch means anyway.
- **If we skip it:** Today `ecs_desired_count` is `1`, so this cannot happen. The
  first time anyone scales to 2 on a new environment, one task crash-loops with an
  error that looks nothing like its cause.

### 9. The database master password sits in local Terraform state

(`terraform/main.tf`, `setup.sh:202-205`)

- **What this is:** Terraform records everything it created in a state file.
- **Problem:** `main.tf` has no `backend` block, so state is a file on whoever ran
  it last. That file holds the RDS master password in plain text, as does
  `terraform/terraform.tfvars`, which `setup.sh` writes. Both are correctly
  gitignored and neither is tracked — I checked — but there is no locking, no
  shared history, and losing the machine loses the state.
- **Fix:** An S3 backend with DynamoDB locking and `encrypt = true`. Keep reading
  the password from SSM Parameter Store, which the ECS task definition already
  does properly.
- **If we skip it:** Two people applying at once can corrupt the infrastructure,
  and the secret lives in an unencrypted file on a laptop.

### 10. The allowed browser origins are hardcoded to localhost

(`SecurityConfig.java:52-59`)

- **What this is:** CORS decides which other websites' pages may call this API.
- **Problem:** The list is `localhost:8080`, `localhost:5173` and `localhost:9000`
  in every profile, with `allowCredentials(true)` and all headers allowed. There
  is no way to configure it. The deployed app survives only because its React page
  is served from the same origin and so never needs CORS.
- **Fix:** Move the list into configuration and set it per profile; leave it empty
  in `prod` if nothing cross-origin is wanted.
- **If we skip it:** The first time anything legitimately calls the API from
  another origin, it fails for a reason nobody will look for in a security class.

### 11. The build depends on snapshot jars from a private group

(`backend/pom.xml:56-60`, `:123-128`)

- **What this is:** A `-SNAPSHOT` version means "whatever was published last", not
  a fixed release.
- **Problem:** `org.example:interface-log:0.0.1-SNAPSHOT` is a compile dependency
  of the running application, and `auth-server-testcontainers:0.1-SNAPSHOT` is a
  test one. The local cache holds three different `interface-log` snapshot
  timestamps, which is the symptom: the same source can build against different
  code on different days.
- **Fix:** Publish fixed versions of both and depend on those.
- **If we skip it:** A build that worked yesterday can fail or behave differently
  today, with nothing in this repo having changed.

### 12. Every controller and service method is logged, and nobody here can see what it logs

(`FormDataController.java:25,32,43`, `FormDataService.java:20,27,40`)

- **What this is:** `@InterfaceLog` is an annotation from that snapshot
  dependency, applied to the classes and again to individual methods.
- **Problem:** It wraps the methods that carry submitted form content and the
  caller's identity. If it logs method arguments — which is the usual reason such
  an annotation exists — then every submission's contents are written to CloudWatch,
  where logs are kept for 7 days. I could not confirm this either way: the
  dependency is a binary jar with no sources in the local cache, so this is a risk
  to check, not a proven leak.
- **Fix:** Read what `interface-log` logs and at which level. If it logs
  arguments, take the annotation off the submission paths or configure its level
  so it is off in `prod`.
- **If we skip it:** Personal data that users typed into forms may be sitting in
  log storage, which is a different system with different access rules from the
  database.

### 13. Nothing tests that a bad submission is rejected

(`backend/src/test/java/.../FormDataServiceTest.java`)

- **What this is:** The submission tests cover ownership well — there are tests
  for the owner updating, a different user being refused, and a missing id.
- **Problem:** There is no test that a submission not matching its form is
  refused, because that behaviour does not exist (finding 2). When finding 2 is
  fixed, this is the test that keeps it fixed.
- **Fix:** With the validation added, one test: a form with a required field, a
  submission missing it, expect the request to be refused.
- **If we skip it:** Validation added later can be removed or broken by a
  refactor with nothing failing.

---

## Nice to have

### 14. `createForm` returns 500 on a duplicate key under concurrency

(`FormController.java:44-49`)

- **What this is:** It checks `exists` then saves — the same two-step pattern as
  finding 8.
- **Problem:** The check makes the common case a clean 400. Two admins creating
  the same key at once get past the check, the unique constraint rejects one, and
  the resulting `DataIntegrityViolationException` falls through to the catch-all
  handler as a 500 "Internal server error".
- **Fix:** Handle `DataIntegrityViolationException` in `GlobalExceptionHandler` as
  409 Conflict.
- **If we skip it:** A rare, confusing 500 instead of a clear "already exists".

### 15. Dead permission rule for a console that cannot run

(`SecurityConfig.java:30`)

- **What this is:** `/h2-console/**` is listed as open to everyone.
- **Problem:** H2 is a `test`-scoped dependency (`backend/pom.xml:80-83`), so the
  console does not exist in a running application and the rule does nothing. It is
  also a standing invitation: if H2 is ever added at runtime, the console is
  reachable with no login and CSRF protection disabled.
- **Fix:** Delete the path from the matcher. One line.
- **If we skip it:** Nothing today. It is a trap for later.

### 16. `formKey` is set twice on every submission

(`FormDataController.java:38` and `FormDataService.java:34`)

- **What this is:** The controller builds `new FormData(key, data, user)`; the
  service then calls `formData.setFormKey(formKey)` with the same value.
- **Fix:** Delete the line in the service, and the comment above it.
- **If we skip it:** A reader has to work out whether the two keys can differ.

### 17. A certificate ARN and AWS account number are hardcoded

(`terraform/alb.tf:46`)

- **What this is:** The HTTPS listener names one specific ACM certificate.
- **Fix:** Make it a variable.
- **If we skip it:** Nobody else can apply this Terraform, and a second
  environment needs the file edited.

### 18. Logs are kept for 7 days

(`terraform/ecs.tf:1-10`)

- **What this is:** `retention_in_days = 7` on the ECS log group.
- **Problem:** Short for investigating anything reported a week late; and if
  finding 12 turns out to be real, it is also the window in which personal data
  sits in logs. Either way the number deserves a deliberate choice.
- **Fix:** Pick a retention that matches how long you would want to investigate.

---

`Verdict:` Not healthy yet. Fix 1 first and today — a deployed instance has a
guessable administrator — then 3 (the database has no backup and no final
snapshot), then 2 (nothing validates what gets stored). The application code
itself is clean and well-tested for its size; nearly all the serious risk is in
the deployment scripts and the Terraform.

`Lean: -40 lines, -0 dependencies possible.` The `locals.test_users` block and the
`test_users` output are dead or should be (findings 1 and 15 together, about 35
lines), plus the `/h2-console/**` matcher and the duplicate `setFormKey`. No
dependency could be shown to be unnecessary — `interface-log` could not be read.

`Not checked:` the React code beyond `src/services/http.ts` (no component or page
was read, so frontend logic, the zod schemas and the i18n keys are unaudited);
`debt-bot/` entirely; the `testcontainers/` module; `docker-compose.yaml`;
`.github/` workflows; `DEPLOY.md`; `deploy.sh` and `cleanup.sh`; `terraform/main.tf`,
`alb.tf` and `iam.tf` other than the lines quoted; the contents of the e2e tests;
and the internals of `interface-log`, which is only available as a compiled jar. No
tests were run and nothing was deployed as part of this audit.
