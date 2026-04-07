# AWS Deployment Plan — Spring Boot + Static Frontend

## Overview

A production-style AWS setup demonstrating ECS Fargate, ECR, S3, CloudFront, Terraform IaC, and GitHub Actions CI/CD. The app can be torn down to near-zero cost and redeployed in minutes entirely from files and GitHub Secrets — no manual AWS console steps after initial setup.

---

## Stack

| Layer | Technology | Notes |
|---|---|---|
| Backend runtime | ECS Fargate | No servers to manage, container-based |
| Backend image | ECR | Private Docker registry |
| Frontend hosting | S3 + CloudFront | Static files, globally cached |
| Infrastructure as Code | Terraform | All infra lives in repo files |
| CI/CD | GitHub Actions | Triggered by push or manually |
| Secrets | GitHub Secrets | AWS keys never in files |

---

## What Lives Where

### In your repository (no console clicking)

```
terraform/
  main.tf           # VPC, subnets, security groups
  ecs.tf            # ECS cluster, Fargate service, task definition
  ecr.tf            # ECR repository
  alb.tf            # Application Load Balancer, target group, listener
  frontend.tf       # S3 bucket + CloudFront distribution
  iam.tf            # IAM roles for ECS task + GitHub Actions user
  variables.tf
  outputs.tf

Dockerfile          # Multi-stage: Maven build → slim JRE image

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

---

## Architecture

### Request flow (when live)

```
User → CloudFront /* → S3 static files
User → CloudFront /api/* → ALB → ECS Fargate → Spring Boot container
```

CloudFront acts as a single entry point for both frontend and backend. The ALB is not directly public — it is only reachable via CloudFront, eliminating CORS entirely.

### Networking

- VPC with public subnets
- Security groups: port 80/443 open on ALB, ALB → ECS on container port only
- ECS tasks run in public subnet with ALB in front

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

### Phase 1 — One-time AWS setup (~30 min)

**Prerequisites:** Install the AWS CLI (`winget install Amazon.AWSCLI`) and configure it with your admin credentials (`aws configure`).

#### Step 1 — Create IAM user for GitHub Actions

```bash
aws iam create-user --user-name github-actions-deploy
```

#### Step 2 — Create and attach a least-privilege policy

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
aws iam put-user-policy \
  --user-name github-actions-deploy \
  --policy-name dynamic-form-deploy-policy \
  --policy-document file://deploy-policy.json
```

#### Step 3 — Generate access keys

```bash
aws iam create-access-key --user-name github-actions-deploy
```

This outputs `AccessKeyId` and `SecretAccessKey` — save them immediately, the secret is only shown once.

#### Step 4 — Create the ECR repository

```bash
aws ecr create-repository \
  --repository-name dynamic-form \
  --region eu-north-1
```

#### Step 5 — Add secrets to GitHub

```bash
gh secret set AWS_ACCESS_KEY_ID --body "<your-access-key-id>"
gh secret set AWS_SECRET_ACCESS_KEY --body "<your-secret-access-key>"
gh secret set AWS_REGION --body "eu-north-1"
gh secret set AWS_ACCOUNT_ID --body "<your-12-digit-account-id>"
gh secret set ECR_REPOSITORY --body "dynamic-form"
```

> **Note:** Replace placeholder values with the actual values from the previous steps. You can find your account ID with `aws sts get-caller-identity --query Account --output text`.

### Phase 2 — Terraform files

Write `terraform/main.tf` covering:

- VPC + public subnets
- Security groups (ALB: 80/443 open; ECS: only from ALB)
- ECR repository
- ECS cluster + Fargate service + task definition (points to ECR image)
- ALB + target group + listener
- S3 bucket (static website) + CloudFront distribution with **two origins**:
  - Default origin: S3 (serves `/*`)
  - Second origin: ALB (serves `/api/*` via an ordered cache behavior with caching disabled and `Host` header forwarded)
- IAM role for ECS task execution
- IAM policy for GitHub Actions user

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
2. Configure AWS credentials from Secrets
3. `docker build` → `docker push` to ECR
4. `aws ecs update-service --force-new-deployment`
5. `aws s3 sync ./frontend/build s3://your-bucket`
6. CloudFront cache invalidation

**destroy.yml** (manual trigger, `workflow_dispatch`):

1. Configure AWS credentials from Secrets
2. `terraform destroy` targeting ECS service, Fargate tasks, and ALB

### Phase 5 — First deploy

Run `terraform apply` once (locally or in Actions) to bootstrap all infrastructure. All subsequent updates flow through GitHub Actions.

---

## AWS Skills Demonstrated

- **ECS Fargate** — container orchestration without managing servers
- **ECR** — private Docker image registry
- **CloudFront + S3** — CDN-backed static hosting
- **Terraform IaC** — all infrastructure in version-controlled files
- **IAM least-privilege** — scoped roles for ECS task and CI/CD
- **ALB** — HTTP load balancer with health checks
- **VPC + subnet design** — public/private network layout
- **GitHub Actions CI/CD** — automated build, push, and deploy pipeline
