---
id: ponytail
status: done
priority: low
kind: task
---

- run ponytail audit, write findings to a file. do not act on findings yet.

# Result

Ran `/ponytail:ponytail-audit` over the whole repo and wrote the report to
`docs/PonytailAuditFindings.md`. 18 findings: 3 must-fix, 10 should-fix, 5
nice-to-have. **No finding was acted on**, as the entry asked.

The three must-fix items, in the order the report recommends:

1. `setup.sh` creates a real Cognito user `admin` with the permanent password
   `admin`, in the `ROLE_ADMIN` group, which `RoleClaimsConverter` turns into full
   admin authority over the API. `terraform/outputs.tf` also prints both demo
   passwords with `sensitive = false`, and `create_test_users` defaults to `true`.
2. The backend never validates a submission against its form definition. The
   `required`, `type` and `options` values on `Field` are read nowhere in main
   code — only the React page validates.
3. The RDS instance has `skip_final_snapshot = true` and no
   `backup_retention_period`, `storage_encrypted` or `deletion_protection`, so a
   replacement or delete loses every submission permanently.

The application code is in good shape for its size and the ownership rules are
properly tested. Almost all the serious risk is in `setup.sh` and `terraform/`.

## How to review

1. Read `docs/PonytailAuditFindings.md` top to bottom — about 200 lines, and each
   finding names its file and line range.
2. Check finding 1 against the source, since it is the one that would need acting
   on today: `setup.sh:296-332` (user creation with `--permanent`),
   `terraform/outputs.tf:76-93` (`sensitive = false`) and
   `backend/.../RoleClaimsConverter.java:29-39` (how `cognito:groups` becomes
   `ROLE_ADMIN`).
3. Confirm finding 2's central claim yourself — that nothing reads the field
   rules — with:

   ```powershell
   Select-String -Path backend\src\main\java -Recurse -Pattern 'isRequired|getRequired|\.required'
   ```

   It returns nothing. That is the evidence the finding rests on.
4. For finding 6, compare `V1__initial_schema.sql:21-23` (indexes on `form_key`
   and `submitted_at`) with `FormDataRepository.java:10` (the query filters on
   `submitted_by`).
5. Read the `Not checked:` list at the end of the report before treating it as
   complete. The React components, `debt-bot/`, the `testcontainers/` module and
   the CI workflows were not read, and no tests were run as part of the audit.

Two caveats worth knowing while reviewing: finding 12 is a **risk, not a proven
leak** — `interface-log` ships only as a compiled jar with no sources in the local
cache, so what it logs could not be confirmed. And finding 8's race cannot happen
at the current `ecs_desired_count = 1`; it needs a scale-up on a fresh database.
