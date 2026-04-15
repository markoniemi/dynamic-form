# AWS Deployment Plan — Spring Boot + Static Frontend

## Overview

A production-style AWS setup demonstrating ECS Fargate, ECR, S3, CloudFront, Terraform IaC, and GitHub Actions CI/CD. The app can be torn down to near-zero cost and redeployed in minutes entirely from files and GitHub Secrets — no manual AWS console steps after initial setup.

---

## Stack

| Layer | Technology | Notes |
|---|---|---|
| Backend runtime | ECS Fargate | No servers to manage, container-based |
| Backend image | ECR | Private Docker registry |
| Frontend hosting | S3 + CloudFront | Static files built by Vite, synced to S3, served via CloudFront CDN |
| Database | RDS PostgreSQL | Managed PostgreSQL; H2 is test-only |
| Infrastructure as Code | Terraform | All infra lives in repo files |
| CI/CD | GitHub Actions | Triggered by push or manually |
| Secrets | GitHub Secrets + AWS SSM | AWS keys and DB credentials never in files |

---

## What Lives Where

### In your repository (no console clicking)

```
terraform/
  main.tf           # VPC, subnets, security groups
  ecs.tf            # ECS cluster, Fargate service, task definition
  ecr.tf            # ECR repository
  alb.tf            # Application Load Balancer, target group, listener
  rds.tf            # RDS PostgreSQL instance + subnet group + security group
  frontend.tf       # S3 bucket + CloudFront distribution (two origins: S3 default + ALB for /api/*)
  iam.tf            # IAM roles for ECS task + GitHub Actions user
  variables.tf
  outputs.tf

Dockerfile          # Multi-stage: Maven build → slim JRE image
.dockerignore       # Excludes frontend/node_modules, .git, target/, etc.

.github/workflows/
  deploy.yml        # Triggered on push to main
  destroy.yml       # Manual trigger only (workflow_dispatch)
```

### In GitHub Secrets (never in files)

| Secret | Value |
|---|---|
| `AWS_ACCESS_KEY_ID` | Least-privilege IAM user key |
| `AWS_SECRET_ACCESS_KEY` | Corresponding secret |
| `AWS_REGION` | e.g. `eu-north-1` |
| `AWS_ACCOUNT_ID` | 12-digit account number |
| `ECR_REPOSITORY` | Repository name |
| `DB_URL` | `jdbc:postgresql://<rds-endpoint>:5432/dynamicform` |
| `DB_USERNAME` | RDS master username |
| `DB_PASSWORD` | RDS master password |
| `S3_BUCKET` | Frontend S3 bucket name |
| `CLOUDFRONT_DISTRIBUTION_ID` | CloudFront distribution ID (for cache invalidation) |

**Note:** `OAUTH2_ISSUER_URI` is now stored in AWS Parameter Store (`/config/oauth2-issuer-uri`) instead of GitHub Secrets — see Phase 1, Step 5.5.

---

## Architecture

### Request flow (when live)

```
User → CloudFront /* (default)  → S3 bucket        → React SPA (static files)
User → CloudFront /api/*        → ALB → ECS Fargate → Spring Boot REST API
                                              ↓
                                        RDS PostgreSQL
```

CloudFront has **two origins**:
- **Default origin (S3):** serves the React SPA for all paths not matched by a more specific behaviour
- **`/api/*` behaviour (ALB):** forwards API requests to the Spring Boot container with caching disabled and the `Host` header forwarded

The ALB is not directly public — it is only reachable via CloudFront, which eliminates CORS entirely. The frontend `fetch` calls hit the same CloudFront domain for both static assets and API requests.

> The frontend is also packaged as a WebJar inside the Spring Boot JAR (that's how the Maven multi-module build works), but the S3/CloudFront path is the live delivery mechanism. The embedded WebJar is not used in production.

### Networking

- VPC with public subnets (ALB, ECS) and private subnets (RDS)
- Security groups:
  - ALB: 80/443 open to internet
  - ECS: only traffic from ALB on container port 8080
  - RDS: only traffic from ECS security group on port 5432
- ECS tasks run in public subnet with ALB in front; RDS in private subnet

---

## Tear Down / Bring Back Up

### Tear down (save money)

Trigger `destroy.yml` manually from the GitHub Actions UI.

```
terraform destroy -target=aws_ecs_service... -target=aws_lb...
```

- ECS tasks stop, load balancer is deleted
- Docker images remain in ECR (cents/month storage)
- Frontend remains in S3 (cents/month storage)
- Cost drops to near zero

### Bring back up

Push to `main` or re-run `deploy.yml`.

- Terraform detects missing ECS service and ALB, recreates them
- Pulls latest image from ECR
- Live again in ~3–5 minutes

---

## Step-by-Step Implementation Plan

### Phase 0 — Application changes (required before deployment)

These changes must be made to the application source code before any AWS infrastructure is useful.

#### 0.1 — Restructure Spring Boot configuration files

Use `application.yaml` for shared values only, with profile-specific files for overrides. If no profile is active the app refuses to start rather than silently connecting to the wrong database.

**`application.yaml`** — shared/common (always loaded):

```yaml
server:
  port: 8080
spring:
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

**`application-dev.yaml`** — local development overrides (`SPRING_PROFILES_ACTIVE=dev`):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/template
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update            # Convenient locally; never use in prod
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000
logging:
  level:
    com.example.backend: DEBUG
```

**`application-prod.yaml`** — production overrides (`SPRING_PROFILES_ACTIVE=prod`):

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate          # Never drop/create in production
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OAUTH2_ISSUER_URI}
```

The existing `application.yaml` currently holds localhost values for datasource and OAuth2 — move those into `application-dev.yaml` and replace `application.yaml` with the shared-only content above. Set `SPRING_PROFILES_ACTIVE=prod` in the ECS task definition (covered in Phase 2).

#### 0.2 — Add database migrations (Flyway)

`ddl-auto: create-drop` (used in dev) destroys data on restart. Production databases must use schema migrations.

Add Flyway to `backend/pom.xml`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Create initial migration `backend/src/main/resources/db/migration/V1__initial_schema.sql` by exporting the schema that Hibernate would generate (`spring.jpa.show-sql=true` + `ddl-auto: create` on a fresh dev DB). All subsequent schema changes go in new versioned migration files (`V2__...`, `V3__...`).

#### 0.3 — Add Maven Jib plugin

Jib builds and pushes a Docker image directly to ECR **without requiring a local Docker daemon**, making it ideal for GitHub Actions CI runners.

Add to `backend/pom.xml` inside `<build><plugins>`:

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>3.4.4</version>
    <configuration>
        <from>
            <image>eclipse-temurin:21-jre-alpine</image>
        </from>
        <to>
            <image>dynamic-form:${project.version}</image>
            <tags>
                <tag>latest</tag>
            </tags>
        </to>
        <container>
            <ports>
                <port>8080</port>
            </ports>
            <environment>
                <SPRING_PROFILES_ACTIVE>prod</SPRING_PROFILES_ACTIVE>
            </environment>
            <jvmFlags>
                <jvmFlag>-Xms256m</jvmFlag>
                <jvmFlag>-Xmx512m</jvmFlag>
            </jvmFlags>
        </container>
    </configuration>
</plugin>
```

**Local build** (Docker daemon):
```bash
mvn -pl backend jib:dockerBuild
```

**CI/AWS deployment** (ECR):
```bash
mvn -pl backend jib:build -DskipTests \
  -Djib.to.image=<AWS_ACCOUNT_ID>.dkr.ecr.<AWS_REGION>.amazonaws.com/<ECR_REPOSITORY>:latest
```

#### 0.4 — Configure OIDC issuer-uri via AWS Parameter Store

The frontend bootstraps by fetching the OIDC issuer URI from `/api/config/oauth2-issuer-uri`. This endpoint should read from AWS Parameter Store in production, but fall back to `application-dev.yaml` locally.

**Update `backend/pom.xml`** — add Spring Cloud AWS:

```xml
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-starter-parameter-store</artifactId>
    <version>3.1.1</version>
</dependency>
```

**Update `ConfigController.java`** — read from the same property Spring Security uses:

```java
@RestController
@RequestMapping("/api/config")
public class ConfigController {

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String oauth2IssuerUri;

  @GetMapping("/oauth2-issuer-uri")
  public String getOauth2IssuerUri() {
    return oauth2IssuerUri;
  }
}
```

**Update `application-dev.yaml`** — keep localhost (works without AWS):

The existing `spring.security.oauth2.resourceserver.jwt.issuer-uri: http://localhost:9000` is unchanged.

**Update `application-prod.yaml`** — read from AWS Parameter Store:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${aws:parameter:/config/oauth2-issuer-uri}
```

The endpoint is already whitelisted in `SecurityConfig.java` (`/api/config/**` permits all), so the frontend can fetch it without authentication.

#### 0.5 — Configure actuator health check

ECS requires a health check endpoint to determine if a container is healthy. Spring Actuator is already on the classpath. Verify `application-prod.yaml` exposes `/actuator/health` (covered in 0.1 above).

The ECS task definition health check should call:

```
GET http://localhost:8080/actuator/health
```

---

### Phase 1 — One-time AWS setup (~30 min)

**Prerequisites:** Install the AWS CLI (`winget install Amazon.AWSCLI`) and configure it with your admin credentials (`aws configure`).

#### Step 1 — Get AWS access keys for the CLI

You need an IAM user with admin permissions to run `aws configure`. If you already have access keys, skip to Step 2.

1. Sign in to the [AWS Console](https://console.aws.amazon.com/) with your root or admin account
2. Go to **IAM → Users → Create user**
3. Give the user a name (e.g. `admin-cli`) and attach the **AdministratorAccess** managed policy
4. Select the user → **Security credentials** tab → **Create access key**
5. Choose **Command Line Interface (CLI)** as the use case
6. Copy the **Access key ID** and **Secret access key** — the secret is only shown once

Now configure the CLI:

```bash
aws configure
```

When prompted, enter the access key ID, secret access key, default region (`eu-north-1`), and output format (`json`).

#### Step 2 — Create IAM user for GitHub Actions

```bash
aws iam create-user --user-name github-actions-deploy
```

#### Step 3 — Create and attach a least-privilege policy

Save the following as `deploy-policy.json`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ECRAccess",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:CreateRepository",
        "ecr:DescribeRepositories"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ECSAccess",
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService",
        "ecs:DescribeServices",
        "ecs:DescribeClusters",
        "ecs:RegisterTaskDefinition",
        "ecs:DeregisterTaskDefinition",
        "ecs:DescribeTaskDefinition",
        "ecs:CreateCluster",
        "ecs:CreateService",
        "ecs:DeleteService"
      ],
      "Resource": "*"
    },
    {
      "Sid": "S3Access",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket",
        "s3:CreateBucket",
        "s3:PutBucketPolicy",
        "s3:PutBucketWebsite"
      ],
      "Resource": "*"
    },
    {
      "Sid": "CloudFrontAccess",
      "Effect": "Allow",
      "Action": [
        "cloudfront:CreateInvalidation",
        "cloudfront:GetDistribution",
        "cloudfront:CreateDistribution",
        "cloudfront:UpdateDistribution"
      ],
      "Resource": "*"
    },
    {
      "Sid": "IAMPassRole",
      "Effect": "Allow",
      "Action": [
        "iam:PassRole",
        "iam:GetRole",
        "iam:CreateRole",
        "iam:AttachRolePolicy"
      ],
      "Resource": "*"
    },
    {
      "Sid": "EC2Networking",
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeVpcs",
        "ec2:DescribeSubnets",
        "ec2:DescribeSecurityGroups",
        "ec2:CreateVpc",
        "ec2:CreateSubnet",
        "ec2:CreateSecurityGroup",
        "ec2:AuthorizeSecurityGroupIngress",
        "ec2:CreateInternetGateway",
        "ec2:AttachInternetGateway",
        "ec2:CreateRouteTable",
        "ec2:CreateRoute",
        "ec2:AssociateRouteTable",
        "ec2:ModifyVpcAttribute"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ALBAccess",
      "Effect": "Allow",
      "Action": [
        "elasticloadbalancing:*"
      ],
      "Resource": "*"
    },
    {
      "Sid": "LogsAccess",
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:DescribeLogGroups"
      ],
      "Resource": "*"
    },
    {
      "Sid": "SSMParameterStore",
      "Effect": "Allow",
      "Action": [
        "ssm:PutParameter",
        "ssm:GetParameter",
        "ssm:GetParameters",
        "ssm:GetParametersByPath"
      ],
      "Resource": "arn:aws:ssm:*:*:parameter/config/*"
    }
  ]
}
```

Then attach it:

```bash
aws iam put-user-policy --user-name github-actions-deploy --policy-name dynamic-form-deploy-policy --policy-document file://deploy-policy.json
```

#### Step 4 — Generate access keys

```bash
aws iam create-access-key --user-name github-actions-deploy
```

This outputs `AccessKeyId` and `SecretAccessKey` — save them immediately, the secret is only shown once.

#### Step 5 — Create the ECR repository

```bash
aws ecr create-repository --repository-name dynamic-form --region eu-north-1
```

#### Step 5.5 — Create AWS Parameter Store parameter for OIDC issuer URI

Store the production OAuth2 issuer URI in Parameter Store so the backend can fetch it at startup:

```bash
aws ssm put-parameter \
  --name /config/oauth2-issuer-uri \
  --value "https://your-production-oauth2-issuer.example.com" \
  --type String \
  --region eu-north-1
```

Replace `https://your-production-oauth2-issuer.example.com` with your actual production OAuth2 issuer URI. The ECS task IAM role (created in Phase 5) will have `ssm:GetParameter` permission to retrieve this value at runtime.

> **Note:** You can update the parameter later without redeploying: `aws ssm put-parameter --name /config/oauth2-issuer-uri --value "new-value" --overwrite`.

#### Step 6 — Add secrets to GitHub

```bash
gh secret set AWS_ACCESS_KEY_ID --body "<your-access-key-id>"
gh secret set AWS_SECRET_ACCESS_KEY --body "<your-secret-access-key>"
gh secret set AWS_REGION --body "eu-north-1"
gh secret set AWS_ACCOUNT_ID --body "<your-12-digit-account-id>"
gh secret set ECR_REPOSITORY --body "dynamic-form"

# Set after RDS is created (Phase 4 / terraform apply)
gh secret set DB_URL --body "jdbc:postgresql://<rds-endpoint>:5432/dynamicform"
gh secret set DB_USERNAME --body "<rds-master-username>"
gh secret set DB_PASSWORD --body "<rds-master-password>"
```

> **Note:** Replace placeholder values with the actual values from the previous steps. You can find your account ID with `aws sts get-caller-identity --query Account --output text`. The RDS endpoint is available after `terraform apply` via `terraform output rds_endpoint`. The `OAUTH2_ISSUER_URI` is now stored in AWS Parameter Store (Step 5.5) instead of GitHub Secrets — the ECS task reads it from Parameter Store at startup.

### Phase 2 — Docker-compose

**`docker-compose.yml`** — spin up the full stack locally with PostgreSQL and OAuth2 server:

```yaml
services:
  auth:
    image: ghcr.io/markoniemi/oauth2-server:latest
    ports:
      - "9000:9000"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/.well-known/openid-configuration"]
      interval: 5s
      timeout: 3s
      retries: 10

  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: dynamicform
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5433:5432"   # 5433 to avoid clashing with a locally running postgres
    depends_on:
      - auth

  app:
    image: dynamic-form:1.0.0-SNAPSHOT
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:postgresql://db:5432/dynamicform
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      OAUTH2_ISSUER_URI: http://auth:9000
    depends_on:
      auth:
        condition: service_healthy
      db:
        condition: service_started
```

**Before running docker-compose, build the app image locally using Jib:**

```bash
# Build and push to local Docker daemon
mvn -pl backend jib:dockerBuild
```

This creates the `dynamic-form:1.0.0-SNAPSHOT` image in your local Docker registry. Then start the stack:

```bash
docker compose up
```

**Note on the `auth` service:** The OAuth2 server container (`ghcr.io/markoniemi/oauth2-server:latest`) is the same image used by `TestcontainersConfig` during integration tests. It provides the authorization server that the backend validates JWTs against. The app connects to it via `OAUTH2_ISSUER_URI: http://auth:9000` (internal Docker network DNS).

**Checkpoint:**
- `mvn -pl backend jib:dockerBuild && docker compose up` — all three services start
- `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`
- `curl http://localhost:9000/.well-known/openid-configuration` returns the OAuth2 metadata (verify auth server is accessible)
- Frontend can authenticate via the OAuth2 server at `http://localhost:9000`

---

### Phase 3 — Terraform: networking

Write `terraform/main.tf`, `terraform/variables.tf`, and `terraform/outputs.tf` covering only networking:

- Provider block (`aws`, region from variable)
- VPC (`10.0.0.0/16`)
- Two public subnets in different AZs (for ALB requirement)
- Two private subnets in different AZs (for RDS)
- Internet Gateway + route table for public subnets
- Security group: `alb-sg` — ingress 80/443 from `0.0.0.0/0`
- Security group: `ecs-sg` — ingress 8080 from `alb-sg` only
- Security group: `rds-sg` — ingress 5432 from `ecs-sg` only

**Checkpoint:** `terraform init && terraform apply` — resources appear in the AWS console with no errors.

---

### Phase 4 — Terraform: database

Write `terraform/rds.tf`:

- `aws_db_subnet_group` using the private subnets from Phase 3
- `aws_db_instance`:
  - `engine = "postgres"`, `engine_version = "16"`
  - `instance_class = "db.t3.micro"` (free tier eligible)
  - `db_name`, `username`, `password` from Terraform variables (fed from `terraform.tfvars`, never committed)
  - `vpc_security_group_ids = [aws_security_group.rds-sg.id]`
  - `skip_final_snapshot = true`
  - `publicly_accessible = false`
- Output `rds_endpoint` for use in GitHub Secrets

Add to `terraform/variables.tf`:
```hcl
variable "db_name" {
  description = "Database name"
  type        = string
  default     = "dynamicform"
}

variable "db_username" {
  description = "Database master username"
  type        = string
}

variable "db_password" {
  description = "Database master password"
  type        = string
  sensitive   = true
}
```

Add to `terraform/outputs.tf`:
```hcl
output "rds_endpoint" {
  description = "RDS instance endpoint (hostname:port)"
  value       = aws_db_instance.main.endpoint
}
```

Create `terraform/terraform.tfvars` (never commit):
```hcl
db_username = "postgres"
db_password = "your-secure-password"
```

Create `terraform/terraform.tfvars.example` (commit this as a template):
```hcl
# Copy this file to terraform.tfvars and set actual values
# DO NOT commit terraform.tfvars to git — it contains secrets

db_username = "postgres"
db_password = "changeme"
```

Update `.gitignore` to exclude credentials:
```
terraform/terraform.tfvars
```

**Step 1: Initialize and plan**

```bash
cd terraform
terraform init  # If not already done
terraform plan
```

**Step 2: Apply**

```bash
terraform apply
```

This will prompt you to confirm. Enter `yes`. RDS creation takes ~5 minutes.

**Step 3: Retrieve endpoint and credentials**

```bash
terraform output rds_endpoint
# Example output: dynamic-form-db.xyz123.eu-north-1.rds.amazonaws.com:5432
```

**Step 4: Add to GitHub Secrets**

Using the endpoint from Step 3 and your credentials from `terraform.tfvars`:

```bash
gh secret set DB_URL --body "jdbc:postgresql://dynamic-form-db.xyz123.eu-north-1.rds.amazonaws.com:5432/dynamicform"
gh secret set DB_USERNAME --body "postgres"
gh secret set DB_PASSWORD --body "your-secure-password"
```

Replace the endpoint with your actual endpoint from `terraform output rds_endpoint`.

**Checkpoint:** RDS instance is created, `terraform output rds_endpoint` returns a hostname with port 5432. GitHub Secrets contain `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

---

### Phase 5 — Terraform: compute + first manual deploy

Write `terraform/ecr.tf`, `terraform/ecs.tf`, and `terraform/alb.tf`:

**`terraform/ecr.tf`:**
- `aws_ecr_repository` named `dynamic-form`

**`terraform/alb.tf`:**
- `aws_lb` (type `application`) in the public subnets, attached to `alb-sg`
- `aws_lb_target_group` for port 8080, protocol HTTP, with health check on `/actuator/health`
- `aws_lb_listener` on port 80 forwarding to the target group

**`terraform/ecs.tf`:**
- `aws_ecs_cluster`
- `aws_ecs_task_definition` — Fargate, 512 CPU / 1024 MB memory, container image from ECR `latest`, environment variables: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `OAUTH2_ISSUER_URI`; health check: `CMD-SHELL curl -f http://localhost:8080/actuator/health`; log driver `awslogs`
- `aws_ecs_service` — desired count 1, launch type FARGATE, network config in public subnets with `ecs-sg`, load balancer attached to the target group

**`terraform/iam.tf`:**
- `aws_iam_role` for ECS task execution with `AmazonECSTaskExecutionRolePolicy`

After `terraform apply`, do a **manual first push** to confirm the pipeline before automating it:

```bash
aws ecr get-login-password --region eu-north-1 | \
  docker login --username AWS --password-stdin \
  <account-id>.dkr.ecr.eu-north-1.amazonaws.com

mvn -pl backend jib:build -DskipTests \
  -Djib.to.image=<account-id>.dkr.ecr.eu-north-1.amazonaws.com/dynamic-form:1.0.0-SNAPSHOT

aws ecs update-service \
  --cluster dynamic-form-cluster \
  --service dynamic-form-service \
  --force-new-deployment \
  --region eu-north-1
```

**Checkpoint:** ALB DNS name (from `terraform output alb_dns`) responds to `GET /actuator/health` with `{"status":"UP"}`.

---

### Phase 6 — Terraform: frontend + first S3 sync

Write `terraform/frontend.tf`:

- `aws_s3_bucket` — block all public access; do NOT enable static website hosting (use OAC instead)
- `aws_cloudfront_origin_access_control` (OAC) for the S3 origin
- `aws_s3_bucket_policy` — grants CloudFront OAC read access
- `aws_cloudfront_distribution` with two origins:
  - **Default origin (S3):** serves `/*`; cache policy `CachingOptimized`; `index.html` TTL overridden to 60s
  - **Ordered cache behaviour `/api/*` (ALB):** TTL = 0, forward all headers and query strings, no caching
- Output `cloudfront_domain` and `cloudfront_distribution_id`

After `terraform apply`, do a **manual first sync**:

```bash
# Build the frontend
mvn -pl frontend generate-resources   # runs npm install + vite build

aws s3 sync frontend/dist/ s3://<bucket-name> --delete

aws cloudfront create-invalidation \
  --distribution-id <distribution-id> \
  --paths "/index.html"
```

Add `S3_BUCKET` and `CLOUDFRONT_DISTRIBUTION_ID` to GitHub Secrets.

**Checkpoint:** CloudFront domain loads the React SPA and API calls via `/api/*` reach the Spring Boot container.

---

### Phase 7 — GitHub Actions CI/CD

Now that everything works manually, automate it.

**`.github/workflows/deploy.yml`** (on push to `master`):

```yaml
on:
  push:
    branches: [master]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: temurin }
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ secrets.AWS_REGION }}
      - name: Login to ECR
        run: |
          aws ecr get-login-password | docker login --username AWS \
            --password-stdin ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.${{ secrets.AWS_REGION }}.amazonaws.com
      - name: Build
        run: mvn install -DskipTests
      - name: Push backend image
        run: |
          mvn -pl backend jib:build -DskipTests \
            -Djib.to.image=${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.${{ secrets.AWS_REGION }}.amazonaws.com/${{ secrets.ECR_REPOSITORY }}:latest
      - name: Deploy ECS
        run: |
          aws ecs update-service \
            --cluster dynamic-form-cluster \
            --service dynamic-form-service \
            --force-new-deployment \
            --region ${{ secrets.AWS_REGION }}
      - name: Sync frontend to S3
        run: aws s3 sync frontend/dist/ s3://${{ secrets.S3_BUCKET }} --delete
      - name: Invalidate CloudFront
        run: |
          aws cloudfront create-invalidation \
            --distribution-id ${{ secrets.CLOUDFRONT_DISTRIBUTION_ID }} \
            --paths "/index.html"
```

**`.github/workflows/destroy.yml`** (manual trigger only):

```yaml
on:
  workflow_dispatch:

jobs:
  destroy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ secrets.AWS_REGION }}
      - uses: hashicorp/setup-terraform@v3
      - run: terraform -chdir=terraform init
      - run: |
          terraform -chdir=terraform destroy -auto-approve \
            -target=aws_ecs_service.app \
            -target=aws_lb.main
```

**Checkpoint:** Push a trivial change to `master` → Actions workflow goes green → CloudFront domain reflects the change.

---

### Phase 8 — Smoke test and tear-down drill

Verify the full lifecycle before treating the setup as production-ready.

**Smoke test checklist:**
- [ ] `GET <cloudfront-domain>/` — returns the React SPA (`200 OK`)
- [ ] `GET <cloudfront-domain>/api/actuator/health` — returns `{"status":"UP"}`
- [ ] Log in via OAuth2 — token accepted by the Spring Boot resource server
- [ ] Submit a form — data persists in RDS across ECS task restarts
- [ ] `docker compose up` still works locally with `SPRING_PROFILES_ACTIVE=dev`

**Tear-down drill:**
1. Trigger `destroy.yml` manually from the GitHub Actions UI
2. Confirm ECS service and ALB are gone (cost drops to near zero)
3. Re-run `deploy.yml` (or push a commit)
4. Confirm site is live again within ~5 minutes

---

## AWS Skills Demonstrated

- **ECS Fargate** — container orchestration without managing servers
- **ECR** — private Docker image registry
- **RDS PostgreSQL** — managed relational database in private subnet
- **S3 + CloudFront** — CDN-backed static hosting for the React SPA; two-origin distribution (S3 default + ALB for `/api/*`)
- **Terraform IaC** — all infrastructure in version-controlled files
- **IAM least-privilege** — scoped roles for ECS task and CI/CD
- **ALB** — HTTP load balancer with health checks against `/actuator/health`
- **VPC + subnet design** — public subnets (ALB, ECS), private subnets (RDS)
- **GitHub Actions CI/CD** — automated build, Jib push to ECR, and ECS deploy pipeline
- **Maven Jib** — Docker-daemon-free image build and push in CI
