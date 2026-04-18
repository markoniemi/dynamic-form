# AWS Deployment Script

One-command deployment of the entire Dynamic Form application to AWS using Terraform, ECR, ECS, and RDS.

## Complete Workflow

### **Step 1: One-Time Setup (AWS + GitHub + RDS)**

Run the setup script once to configure everything:

**Git Bash:**
```bash
bash setup.sh
```

**Windows Command Prompt:**
```bash
setup.bat
```

This script will:
- ✅ Create IAM user (`github-actions-deploy`)
- ✅ Create and attach IAM policy
- ✅ Generate AWS access keys
- ✅ **Create RDS PostgreSQL database** (saves password in `terraform/terraform.tfvars`)
- ✅ Create Cognito User Pool for OAuth2
- ✅ Set GitHub secrets automatically

**Prerequisites for setup:**
- AWS CLI configured with **admin credentials** (`aws configure`)
- GitHub CLI installed and authenticated (`gh auth login`)

**During setup you'll be prompted for:**
- RDS master password (saved securely in terraform/terraform.tfvars)

### **Step 2: Prepare Application (Phase 0)**

Complete Phase 0 from [AWSDeployPlan.md](docs/AWSDeployPlan.md):
- Restructure Spring Boot configuration (application.yaml, profiles)
- Add Flyway database migrations
- Add Maven Jib plugin
- Configure OAuth2 issuer URI endpoint
- Configure actuator health check

### **Step 3: Deploy Application**

Run the deploy script:

```bash
bash deploy.sh postgres
```

The deployment will:
- ✅ Auto-derive AWS Account ID
- ✅ Auto-derive RDS endpoint (from Phase 1)
- ✅ Auto-create/reuse Cognito pool
- ✅ Create SSM parameters
- ✅ Deploy application infrastructure (compute, ALB, ECS)
- ✅ Build and push Docker image
- ✅ Deploy to ECS
- ✅ Verify health check

---

## Quick Start (If Already Set Up)

After running `setup.sh` once, deploy is simple:

```bash
bash deploy.sh postgres
```

That's it! RDS password is already saved from setup.sh in `terraform/terraform.tfvars`.

## Testing with Cognito Test Users

### Test Users (Enabled by Default)

Test users are **created automatically** by default when you deploy. To disable them:

**To disable test users (if you don't want them):**

```bash
CREATE_TEST_USERS=false bash deploy.sh postgres
```

Or via Terraform:
```bash
cd terraform
terraform apply -var="create_test_users=false"
cd ..
```

**Default behavior:** Test users are created automatically

This creates two test users with roles:

| Username | Password | Email | Role |
|----------|----------|-------|------|
| `admin` | `admin` | admin@example.com | `ROLE_ADMIN` |
| `user` | `user` | user@example.com | `ROLE_USER` |

View credentials after deployment:
```bash
cd terraform
terraform output test_users
cd ..
```

### Testing OAuth2 Login

1. Go to: `http://<alb-dns>/`
2. Click "Login with OAuth2"
3. Use any test user credentials above
4. You'll be redirected to Cognito login
5. After login, redirected back to the app

### Remove Test Users

If you don't want test users, disable them:

```bash
bash deploy.sh postgres -e CREATE_TEST_USERS=false
```

Or via Terraform:
```bash
cd terraform
terraform apply -var="create_test_users=false"
cd ..
```

---

## Configuration — Optional!

No configuration file needed! Just pass the RDS password as an argument:

```bash
bash deploy.sh your-rds-password
```

**Everything else is auto-derived or auto-created:**

| Setting | Status | Source |
|---------|--------|--------|
| `AWS_ACCOUNT_ID` | ✅ Auto-derived | `aws sts get-caller-identity` |
| `AWS_REGION` | ✅ Auto-default | `eu-north-1` |
| `RDS_ENDPOINT` | ✅ Auto-derived | Terraform state |
| `RDS_USERNAME` | ✅ Auto-default | `postgres` |
| `RDS_PASSWORD` | ✅ From argument | `bash deploy.sh <password>` |
| `Cognito User Pool` | ✅ Auto-created | Creates if doesn't exist |
| `COGNITO_ISSUER_URI` | ✅ Auto-derived | Cognito pool ID |

**Optional: Create `deploy.env` to customize defaults**

Only if you need non-default settings:
```bash
# deploy.env (optional)
AWS_REGION=eu-central-1            # Override default
ENVIRONMENT=my-app                 # Override default
TERRAFORM_AUTO_APPROVE=false       # Review plan before applying
```

Then run:
```bash
bash deploy.sh your-rds-password deploy.env
```

## What the Script Does

1. **Validates Configuration** — Checks all required variables are set
2. **Creates SSM Parameters** — Stores secrets in AWS Parameter Store:
   - `/config/db-url`
   - `/config/db-username`
   - `/config/db-password`
   - `/config/oauth2-issuer-uri`
3. **Deploys Infrastructure** — Runs `terraform apply`:
   - VPC, subnets, security groups
   - RDS PostgreSQL
   - ECR repository
   - ALB and ECS cluster
4. **Builds & Pushes Docker Image** — Uses Maven Jib to build and push to ECR
5. **Triggers ECS Redeployment** — Forces ECS to pull the new image
6. **Verifies Health Check** — Waits for the service to be healthy

## Usage Examples

### Production Deployment (Simplest)
```bash
bash deploy.sh your-rds-password
```
That's it! No files needed.

### Environment Variable (Good for CI/CD)
```bash
export RDS_PASSWORD=your-rds-password
bash deploy.sh
```

### Dry Run (Review Terraform Plan)
```bash
# Create config with TERRAFORM_AUTO_APPROVE=false
echo "TERRAFORM_AUTO_APPROVE=false" > deploy.env
bash deploy.sh your-rds-password deploy.env

# Review the terraform plan, then apply manually:
cd terraform && terraform apply
```

### Different Regions/Environments
```bash
# Production (eu-north-1, default)
bash deploy.sh prod-rds-password

# Staging (eu-central-1)
echo "AWS_REGION=eu-central-1" > deploy.staging.env
bash deploy.sh staging-rds-password deploy.staging.env

# Development (with auto-approve disabled)
echo "TERRAFORM_AUTO_APPROVE=false" > deploy.dev.env
bash deploy.sh dev-rds-password deploy.dev.env
```

### CI/CD Pipeline
```bash
# In GitHub Actions or similar:
bash deploy.sh ${{ secrets.RDS_PASSWORD }}
```

## Prerequisites

**CLI Tools:**
- ✅ **AWS CLI** — `aws configure` with **admin credentials**
- ✅ **GitHub CLI** — `gh auth login` (authenticated)
- ✅ **Docker** — Running daemon (for Jib build)
- ✅ **Maven** — With Java 21+ installed
- ✅ **Terraform** — 1.0+
- ✅ **curl** — For health checks
- ✅ **Bash** — 4.0+ (Linux/macOS) or WSL (Windows)

**Initial Setup (One-Time):**
- ⚠️ **Phase 1 Setup** — Run `bash setup.sh` to create IAM user, policy, and GitHub secrets
- ✅ **Phase 0** — Application configuration (application.yaml, Flyway, Jib plugin, ConfigController, Actuator)
- ✅ **Phase 2** (optional) — Docker-compose (only needed for local testing)

**Per-Deployment:**
- ✅ **Phase 4** — RDS instance with known password
- ✅ **RDS_PASSWORD** — Master password for the RDS database

## Troubleshooting

### Script fails on "Health check"
```bash
# Check ECS task logs
aws logs tail /ecs/dynamic-form-app --follow

# Manually check the ALB
curl http://<alb-dns>/actuator/health
```

### ECR login fails
```bash
aws ecr get-login-password --region eu-north-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.eu-north-1.amazonaws.com
```

### Terraform state issues
```bash
cd terraform
rm -rf .terraform terraform.tfstate*
terraform init
```

## Environment Variables in Terraform

The script doesn't require additional Terraform variables because it uses AWS Parameter Store for secrets. However, you can override Terraform variables:

```bash
# In deploy.sh, modify the deploy_terraform() function:
terraform apply -auto-approve \
    -var="ecs_desired_count=2" \
    -var="aws_region=$AWS_REGION"
```

## Customization

### Disable Auto-Approve
```bash
# In deploy.env
TERRAFORM_AUTO_APPROVE=false

# Script will show plan and exit — review, then run manually:
cd terraform && terraform apply
```

### Custom Health Check Timeout
```bash
# In deploy.env
HEALTH_CHECK_TIMEOUT=300  # 5 minutes
HEALTH_CHECK_WAIT=60      # Wait 1 minute before checking
```

### Redeploy Only (Skip Terraform)
```bash
# Edit deploy.sh, comment out deploy_terraform() and capture_terraform_outputs()
# Then run the script to rebuild and redeploy the image
```

## Safety Features

- ✅ **Exit on error** — Script stops at first failure
- ✅ **Minimal secrets** — Only `RDS_PASSWORD` is required (true secret)
- ✅ **Auto-derivation** — AWS Account ID, RDS endpoint, Cognito setup auto-derived
- ✅ **Idempotent** — Can be re-run safely; Cognito pool reused if exists
- ✅ **Health checks** — Verifies the service is running after deploy
- ✅ **Logs** — Captures detailed logs in `/tmp/`
- ✅ **Secrets** — `deploy.env` is .gitignored (never committed)
- ✅ **Summary output** — Shows final configuration before deploying

## Cleanup & Teardown

### Complete Cleanup (Delete Everything)

To test deployment from scratch or avoid AWS costs:

**Interactive (asks for confirmation):**
```bash
bash cleanup.sh
```

**Non-interactive (destroys immediately):**
```bash
bash cleanup.sh --force
```

**On Windows (Git Bash):**
```bash
cleanup.bat              # Interactive
cleanup.bat --force      # Non-interactive
```

The script will:
- ✅ Delete all Terraform resources (ECS, ALB, RDS, VPC, etc.)
- ✅ Clear SSM parameters
- ✅ Remove ECR images (keeps repository for reuse)
- ✅ Clean up local terraform state

**Cost after cleanup:** ~$0/month

Then redeploy from scratch:
```bash
bash deploy.sh postgres
```

## See Also

- [AWSDeployPlan.md](docs/AWSDeployPlan.md) — Detailed Phase 1–8 breakdown
- [aws-architecture.puml](docs/aws-architecture.puml) — Architecture diagram
