# AWS Deployment: Analysis and Improvement Plan

Read-only review, 2026-10-15. Covers `terraform/*.tf`, `setup.sh`, `deploy.sh`, `cleanup.sh`, `certificate.sh`, `DEPLOY.md`, `docs/deploy-policy.json`. Nothing was applied, and no AWS account was touched. `terraform validate` could not run (the AWS provider is not installed under `terraform/.terraform`); `terraform fmt -check` flags `main.tf` and `cognito-users.tf`.

This extends `ReviewFindings.md` items 4, 5, 6, 12, 13, 14 and 18. It does not repeat them. Do those first where they overlap with Phase 1 below.

## Short answers

| Question | Answer |
|---|---|
| Are the Terraform files feasible? | Yes for the network, RDS, ECR, ALB and ECS (a normal Fargate setup). **No for login**: Cognito is only half built (see "Blocker"). The state is local, and the plan has a few likely errors (below). |
| Are the scripts valid? | They run, but they are not correct. `deploy.sh` reports a false "health check passed". `setup.sh` and `cleanup.sh` create and leak resources that Terraform does not know about. `DEPLOY.md` documents files and flags that do not exist. |
| Can they be simpler? | Yes, a lot. Move everything AWS-side into Terraform. Of 1,000 lines of bash, about 30 remain (build, push, redeploy, wait). |
| Can scripts deploy certificates and tokens? | Certificates: yes, `certificate.sh` already does it with `acm import-certificate`. But Terraform does it better, and with a domain or CloudFront the cert is trusted and renews itself. Tokens/secrets: yes, through Terraform (RDS-managed secret, Cognito client) and GitHub OIDC instead of stored AWS keys. User login tokens are issued by Cognito at runtime; nothing to deploy. |
| How do I read app and AWS logs? Can I use an MCP? | `aws logs tail` already works and Claude can run it through Bash. An MCP is optional: `awslabs.cloudwatch-mcp-server` for log queries. Details below. |

## Blocker: login cannot work on Cognito as built

`setup.sh` creates a user pool and users, but no **app client** and no **hosted-UI domain**. The frontend hard-codes `client_id: 'frontend-client'` (`frontend/src/services/configClient.ts:17`). Cognito generates client IDs; you cannot choose one. Without a domain there is no `/oauth2/authorize` endpoint to redirect to. So the deployed app cannot log anyone in. This matches the commit message "AWS deploy improved, not yet working".

More Cognito gaps:
- Cognito's discovery document has no `end_session_endpoint`. `Navigation.tsx:15` calls `signoutRedirect()`, which will throw. Logout needs Cognito's `/logout?client_id=...&logout_uri=...`.
- Access tokens have `username`, not `preferred_username`. `SecurityUtils.getUsername` falls back to `sub` (a UUID), so submissions will show a UUID as owner. Read `username` too.
- Roles: `RoleClaimsConverter` already handles `cognito:groups`. Groups `ROLE_ADMIN` / `ROLE_USER` must exist (setup.sh creates them by CLI).
- Callback URLs in Cognito must be `https://` (except localhost), so a trusted HTTPS URL is required anyway.

## Terraform review

Good: the layout is clear, security groups chain ALB -> ECS -> RDS, RDS is private, ECR scans on push, the HTTP listener redirects, secrets come from SSM and not plain env vars.

Problems, most serious first:
1. **Local state** with `db_password` inside (also `ReviewFindings` 5).
2. **Cognito and certificates live outside Terraform** (CLI scripts), so `terraform destroy` leaks the user pool, IAM user, ACM cert and GitHub secrets. `cognito-users.tf` is dead code.
3. `rds.tf`: `allocated_storage = 10`. RDS PostgreSQL's minimum is 20 GiB, so apply will probably fail with an invalid-storage error. Check with `terraform plan`. `db_name` is a variable, but `deploy.sh` hard-codes `/dynamicform` in the JDBC URL.
4. `alb.tf`: hard-coded cert ARN with account ID; old TLS policy (`ReviewFindings` 12). The ALB DNS name cannot get a trusted ACM cert, because AWS does not issue certs for `*.elb.amazonaws.com`.
5. `ecs.tf`: `:latest` image tag; no `deployment_circuit_breaker`; no `health_check_grace_period_seconds`. Spring takes about 30 s to start and the ALB check needs 2 passes at 30 s, so slow starts get killed. No `enable_execute_command`.
6. `ecr.tf`: the lifecycle rule only matches tag prefix `latest`, so it never expires anything real. Use `tagStatus = "any"`.
7. `iam.tf`: the task role has policies it never uses (SSM, `logs:CreateLogGroup`); `awslogs` uses the execution role. Remove the task role policies, or the role.
8. Style: `terraform fmt` fails; two public and two private subnets copy-pasted (use `count` + `cidrsubnet`); tags repeated everywhere (use provider `default_tags`); provider `~> 5.0`; `outputs.tf` has 8 outputs nobody uses; `terraform.tfvars.example` and the real `terraform.tfvars` hold `postgres`/`postgres`.
9. ECS tasks run in public subnets with public IPs. That is fine and cheap (no NAT, about 30 USD/month saved), and the SG only allows the ALB. Keep it, and document it as intended.

## Script review

**setup.sh (445 lines)**
- Creates an IAM user and key, prints the secret to the terminal, stores it in GitHub (`ReviewFindings` 14). The only workflow never uses it.
- Runs the full `terraform apply` (including the ECS service) before the image and SSM parameters exist, so tasks fail to start until `deploy.sh` runs.
- Cognito code is duplicated in `deploy.sh`. `list-user-pools --max-results 10` does not paginate. `declare -A` needs bash 4 (breaks on stock macOS).
- `local x=$(cmd)` hides failures from `set -e`. Password given as a CLI argument ends up in shell history and `ps`.
- Seeds `admin/admin` and `user/user` into the real pool (`ReviewFindings` 13).

**deploy.sh (371 lines)**
- **False health check:** it curls `http://ALB/actuator/health`; the ALB answers 301 -> HTTPS; `curl -sf` treats 301 as success. It would "pass" with a dead app. Replace with `aws ecs wait services-stable`.
- `terraform plan` output is written to `/tmp` and never used; `apply -auto-approve` plans again. Use `plan -out` + `apply tfplan` (skill rule 8).
- The RDS password lives in two places (argument and `terraform.tfvars`) and can drift.
- SSM parameters are written by bash before Terraform runs, with names assumed in `ecs.tf`. Two files must stay in sync by hand.
- Jib does not need a Docker daemon (`jib:build` pushes directly); the docs say it does.

**cleanup.sh**: leaves the Cognito pool, IAM user/key, ACM cert and GitHub secrets. Says "ECR repository kept" but `terraform destroy` deletes it (it is in state). Deletes `.terraform.lock.hcl`, which should be committed. Region hard-coded.

**certificate.sh**: works (self-signed, 365 days, `acm import-certificate`). Limits: CN `localhost` does not match the ALB host, so browsers warn; imported certs **do not auto-renew** (expires about 2027-04); the key is written into the git tree (`ReviewFindings` 6); the ARN must be pasted into `alb.tf` by hand.

**DEPLOY.md**: references `setup.bat`, `cleanup.bat`, `bash deploy.sh postgres -e ...` and SSM names like `/config/db-url`. None exist. `docs/deploy-policy.json` only allows image deploy, not Terraform, which is fine for CI but it grants `ssm:PutParameter` on `*`.

## Target design

Terraform owns every AWS resource. Bash only builds and ships the image.

```
terraform/            one root, S3 backend, default_tags, count for subnets
  cognito.tf          user pool, app client (public, PKCE, callback = site URL), domain, groups
  secrets: RDS manage_master_user_password = true -> Secrets Manager (no password in tfvars/state/SSM)
  ecs.tf              DB_URL, DB_USERNAME, OAUTH2_ISSUER_URI, OAUTH2_CLIENT_ID as plain env;
                      DB_PASSWORD from the RDS secret (valueFrom "<arn>:password::")
  tls.tf              see "Certificates" below
  github-oidc.tf      OIDC provider + role: ECR push to one repo, ecs:UpdateService on one service
deploy.sh             ~30 lines: ecr login, mvn jib:build with git-SHA tag, register/update service, wait stable
```

Removed: SSM parameters and policies, `setup.sh`, `certificate.sh`, `cleanup.sh` (use `terraform destroy`), the IAM user and key, GitHub secrets, `cognito-users.tf`.

The backend returns the client ID with the issuer (extend `ConfigController`; the frontend stops hard-coding it).

### Certificates (pick one)

- **A. You have a domain:** `aws_acm_certificate` with DNS validation + Route 53 records + `aws_acm_certificate_validation`. Trusted, auto-renewing, no scripts. Best.
- **B. No domain:** put CloudFront in front of the ALB. `*.cloudfront.net` has a trusted cert for free. ALB keeps an HTTP listener limited to the CloudFront managed prefix list plus a secret header. Cognito callback = the CloudFront URL. No browser warning, no cert management.
- **C. Keep self-signed:** use `tls_self_signed_cert` + `aws_acm_certificate` in Terraform and delete `certificate.sh`. Still a warning, and a TLS key in state. Only for throwaway demos.

### Tokens and secrets

- Database: RDS-managed Secrets Manager secret, execution role allowed `secretsmanager:GetSecretValue` on that one ARN.
- CI: GitHub OIDC role, no stored keys. Store only `AWS_ROLE_ARN` as a repo variable (`gh variable set`). Add a deploy job to `maven-build.yaml` on `master`.
- Cognito users: no `admin/admin`. Create the first admin with a generated password (`random_password`, shown once as a sensitive output), or invite by email.

## Reading logs

Group: `/ecs/dynamic-form-app` (7-day retention). Region `eu-north-1`.

```bash
aws logs tail /ecs/dynamic-form-app --follow --since 30m
aws logs tail /ecs/dynamic-form-app --since 2h --filter-pattern ERROR
aws ecs describe-services --cluster dynamic-form-cluster --services dynamic-form-service --query 'services[0].events[:5]'
aws ecs list-tasks --cluster dynamic-form-cluster --desired-status STOPPED       # then describe-tasks -> stoppedReason
aws elbv2 describe-target-health --target-group-arn <arn>
```

What is missing for good diagnosis:
- `logback-spring.xml` uses `%highlight(...)` with a console pattern and no timestamp, so CloudWatch fills with ANSI colour codes and has no app-side time. Add a `prod` springProfile with a plain pattern (or JSON) that includes `%d`.
- `com.example` logs at DEBUG in prod, and `interface-log` logs method calls (may include payloads, `ReviewFindings` "Not checked"). Set INFO in prod.
- Enable `enabled_cloudwatch_logs_exports = ["postgresql"]` on RDS, ALB access logs to S3, and `enable_execute_command` on the service (then `aws ecs execute-command` gives a shell).
- Add a CloudWatch alarm on 5xx count and on "running tasks < 1".

### MCP

Not required: Claude already runs the CLI above. An MCP adds structured Logs Insights queries and metrics without writing CLI syntax. Options from AWS Labs, run with `uvx` (needs `uv` installed):

- `awslabs.cloudwatch-mcp-server`: logs, metrics, alarms. Its use with Claude Code via `uvx` is confirmed in [AWS's Claude Code guide](https://builder.aws.com/content/39tZA5QHRDqisSTJj9dsuKV8ZZW/improving-your-aws-development-workflow-on-claude-code-with-mcp-servers).
- ECS and the generic `aws-api` server also exist in AWS Labs. I could not confirm their exact package names or flags; check the awslabs README before adding them.

Sketch for `.mcp.json` (not added; give it a read-only profile):

```json
"aws-cloudwatch": {
  "type": "stdio",
  "command": "uvx",
  "args": ["awslabs.cloudwatch-mcp-server@latest"],
  "env": { "AWS_PROFILE": "dynamic-form-readonly", "AWS_REGION": "eu-north-1" }
}
```

Pin the version once chosen, as done for Playwright (`ReviewFindings` 19). Use a read-only IAM profile (`CloudWatchLogsReadOnlyAccess` plus `AmazonECS_ReadOnly`): logs can contain user data and tokens.

## Plan

Phase 1: make login and deploy actually work
- [ ] Add S3 backend + lock to Terraform (bootstrap once); remove `terraform.tfstate` from the laptop
- [ ] Fix `allocated_storage` (20), run `terraform fmt`, `init`, `validate`, `plan`
- [ ] Move Cognito into Terraform: pool, public app client (PKCE, callback/logout URLs), domain, groups
- [ ] `ConfigController` also returns client ID; frontend reads it; fix logout for Cognito; read `username` claim
- [ ] Choose certificate option A or B; delete `certificate.sh` and the hard-coded ARN

Phase 2: remove scripts and secrets
- [ ] RDS `manage_master_user_password`; drop SSM parameters, SSM policies and `db_password` variable
- [ ] Rewrite `deploy.sh` (~30 lines, git-SHA tag, `ecs wait services-stable`); delete `setup.sh`, `cleanup.sh`
- [ ] GitHub OIDC role and a deploy job in `maven-build.yaml`; delete IAM user and secrets
- [ ] `create_test_users` default `false`; generated admin password
- [ ] Rewrite `DEPLOY.md` to match (4 commands: `terraform apply`, `deploy.sh`, `terraform output`, `terraform destroy`)

Phase 3: hardening and operations
- [ ] ReviewFindings 5, 6, 12 (RDS safety, key in git, IAM and TLS policy)
- [ ] Circuit breaker, health-check grace period, ECR lifecycle `any`, `enable_execute_command`
- [ ] Prod logback pattern, INFO level, RDS and ALB logs, two alarms
- [ ] Optional: CloudWatch MCP in `.mcp.json` with a read-only profile
- [ ] Optional: split state per domain (network / data / app) only if a second person or environment appears; for one small app one state is fine

## Not checked

- Nothing was run against AWS; `terraform validate/plan` not run. The storage-size error (item 3) and the Cognito logout failure are read from code and AWS rules, not observed.
- Whether the Cognito pool, ALB or cert from earlier runs still exist in the account.
- Cost figures are rough.
