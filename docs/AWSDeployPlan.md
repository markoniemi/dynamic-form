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
| `OAUTH2_ISSUER_URI` | Production OAuth2 issuer URI |
| `S3_BUCKET` | Frontend S3 bucket name |
| `CLOUDFRONT_DISTRIBUTION_ID` | CloudFront distribution ID (for cache invalidation) |

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
            <image>${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}:${project.version}</image>
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

CI command to build and push: `mvn -pl backend jib:build -DskipTests`

> **Note:** Jib and the Dockerfile in Phase 3 are alternative approaches. Jib is preferred in CI because it needs no Docker installation. The Dockerfile is useful for local testing with `docker run`.

#### 0.4 — Configure actuator health check

ECS requires a health check endpoint to determine if a container is healthy. Spring Actuator is already on the classpath. Verify `application-prod.yaml` exposes `/actuator/health` (covered in 0.1 above).

The ECS task definition health check should call:

```
GET http://localhost:8080/actuator/health
```

#### 0.5 — Add `.dockerignore`

Create `.dockerignore` at the repo root to prevent bloated images when using the Dockerfile approach:

```
.git
.github
frontend/node_modules
frontend/.vite
backend/target
terraform
docs
*.md
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

#### Step 6 — Add secrets to GitHub

```bash
gh secret set AWS_ACCESS_KEY_ID --body "<your-access-key-id>"
gh secret set AWS_SECRET_ACCESS_KEY --body "<your-secret-access-key>"
gh secret set AWS_REGION --body "eu-north-1"
gh secret set AWS_ACCOUNT_ID --body "<your-12-digit-account-id>"
gh secret set ECR_REPOSITORY --body "dynamic-form"

# Set after RDS is created (Phase 2 / terraform apply)
gh secret set DB_URL --body "jdbc:postgresql://<rds-endpoint>:5432/dynamicform"
gh secret set DB_USERNAME --body "<rds-master-username>"
gh secret set DB_PASSWORD --body "<rds-master-password>"
gh secret set OAUTH2_ISSUER_URI --body "<your-production-oauth2-issuer>"
```

> **Note:** Replace placeholder values with the actual values from the previous steps. You can find your account ID with `aws sts get-caller-identity --query Account --output text`. The RDS endpoint is available after `terraform apply` via `terraform output rds_endpoint`.

### Phase 2 — Terraform files

Write the following Terraform files:

**`terraform/main.tf`** — VPC + subnets + security groups:
- VPC with public subnets (ALB, ECS) and **private subnets** (RDS)
- Security group: ALB accepts 80/443 from internet
- Security group: ECS accepts 8080 only from ALB security group
- Security group: RDS accepts 5432 only from ECS security group

**`terraform/ecr.tf`** — ECR repository

**`terraform/rds.tf`** — RDS PostgreSQL:
- `aws_db_instance` with `engine = "postgres"`, `engine_version = "16"`
- Instance class `db.t3.micro` (free tier eligible)
- `db_subnet_group` using the private subnets
- `skip_final_snapshot = true` for dev/demo teardown convenience
- Store credentials via `aws_ssm_parameter` and inject into ECS task as environment variables

**`terraform/ecs.tf`** — ECS cluster + Fargate service + task definition:
- Task definition passes `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `OAUTH2_ISSUER_URI`, `SPRING_PROFILES_ACTIVE=prod` as environment variables (sourced from Terraform outputs / SSM)
- Health check: `GET /actuator/health` with 30s interval, 3 retries, 60s start period

**`terraform/alb.tf`** — ALB + target group + listener (port 80 → ECS port 8080)

**`terraform/frontend.tf`** — S3 bucket + CloudFront distribution with **two origins**:
- S3 bucket: static website hosting, not public — access granted only via CloudFront OAC (Origin Access Control)
- CloudFront default origin: S3 (serves `/*` — the React SPA)
- CloudFront ordered cache behaviour: `/api/*` → ALB, with TTL = 0, caching disabled, all headers forwarded
- S3 origin caches aggressively (Vite outputs content-hashed filenames); `index.html` gets a short TTL so new deploys are picked up quickly

**`terraform/iam.tf`** — IAM execution role for ECS + GitHub Actions user policy

### Phase 3 — Dockerfile

Multi-stage build to keep image small:

```dockerfile
# Stage 1: build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Phase 4 — GitHub Actions workflows

**deploy.yml** (on push to main):

1. Checkout code
2. Set up Java 21 (Temurin)
3. Configure AWS credentials from Secrets
4. Authenticate Docker to ECR: `aws ecr get-login-password | docker login`
5. Build full project: `mvn install -DskipTests` (builds backend JAR + frontend `dist/`)
6. Build and push backend image via Jib (no Docker daemon needed):
   ```bash
   mvn -pl backend jib:build \
     -DAWS_ACCOUNT_ID=${{ secrets.AWS_ACCOUNT_ID }} \
     -DAWS_REGION=${{ secrets.AWS_REGION }} \
     -DECR_REPOSITORY=${{ secrets.ECR_REPOSITORY }}
   ```
7. `aws ecs update-service --force-new-deployment` to pick up the new image
8. Sync frontend static files to S3:
   ```bash
   aws s3 sync frontend/dist/ s3://${{ secrets.S3_BUCKET }} --delete
   ```
9. Invalidate CloudFront cache so users get the new `index.html` immediately:
   ```bash
   aws cloudfront create-invalidation \
     --distribution-id ${{ secrets.CLOUDFRONT_DISTRIBUTION_ID }} \
     --paths "/index.html"
   ```
   Content-hashed assets (`/assets/*`) do not need invalidation — their filenames change on each build.

**destroy.yml** (manual trigger, `workflow_dispatch`):

1. Configure AWS credentials from Secrets
2. `terraform destroy` targeting ECS service, Fargate tasks, and ALB

### Phase 5 — First deploy

Run `terraform apply` once (locally or in Actions) to bootstrap all infrastructure. All subsequent updates flow through GitHub Actions.

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
