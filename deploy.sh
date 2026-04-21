#!/bin/bash

################################################################################
# AWS Deployment Script — Complete Infrastructure & Application Deploy
#
# Usage:
#   ./deploy.sh                 # Uses deploy.env
#   ./deploy.sh deploy.prod.env # Uses specific config file
#
# Prerequisites:
#   - AWS CLI configured with credentials
#   - Docker daemon running (for Jib build)
#   - Maven installed
#   - curl installed
#   - Terraform installed
#
# This script performs:
#   1. Validates configuration
#   2. Creates/updates AWS SSM Parameter Store entries
#   3. Deploys infrastructure via Terraform
#   4. Builds and pushes Docker image to ECR
#   5. Triggers ECS service deployment
#   6. Verifies health check
################################################################################

set -euo pipefail  # Exit on error, undefined variables, pipe failures

# ─── Color output ───────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ─── Logging functions ──────────────────────────────────
log_info() {
    echo -e "${BLUE}ℹ${NC} $*"
}

log_success() {
    echo -e "${GREEN}✓${NC} $*"
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $*"
}

log_error() {
    echo -e "${RED}✗${NC} $*" >&2
}

# ─── Parse command-line arguments ──────────────────────
# Usage:
#   deploy.sh <rds-password>                    # Uses deploy.env (optional)
#   deploy.sh <rds-password> <config-file>     # Uses custom config file
#   RDS_PASSWORD=<password> deploy.sh           # Via environment variable

RDS_PASSWORD_ARG="${1:-}"
CONFIG_FILE="${2:-deploy.env}"

# If RDS_PASSWORD provided as argument, use it
if [[ -n "$RDS_PASSWORD_ARG" ]]; then
    RDS_PASSWORD="$RDS_PASSWORD_ARG"
    log_info "Using RDS password from command-line argument"
fi

# Load additional configuration from file if it exists
if [[ -f "$CONFIG_FILE" ]]; then
    log_info "Loading additional configuration from: $CONFIG_FILE"
    source "$CONFIG_FILE"
else
    log_info "No configuration file found ($CONFIG_FILE) — using defaults and environment variables"
fi

# ─── Set defaults for optional variables ────────────────
RDS_USERNAME=${RDS_USERNAME:-postgres}
ENVIRONMENT=${ENVIRONMENT:-dynamic-form}
TERRAFORM_DIR=${TERRAFORM_DIR:-terraform}
TERRAFORM_AUTO_APPROVE=${TERRAFORM_AUTO_APPROVE:-true}
HEALTH_CHECK_ENDPOINT=${HEALTH_CHECK_ENDPOINT:-/actuator/health}
HEALTH_CHECK_TIMEOUT=${HEALTH_CHECK_TIMEOUT:-120}
HEALTH_CHECK_WAIT=${HEALTH_CHECK_WAIT:-30}
AWS_REGION=${AWS_REGION:-eu-north-1}
CREATE_TEST_USERS=${CREATE_TEST_USERS:-true}

# ─── Auto-derive AWS Account ID ─────────────────────────
derive_aws_account_id() {
    if [[ -z "${AWS_ACCOUNT_ID:-}" ]]; then
        log_info "Auto-deriving AWS Account ID..."
        AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text) || {
            log_error "Failed to get AWS Account ID. Check AWS credentials."
            exit 1
        }
        log_info "  AWS Account ID: $AWS_ACCOUNT_ID"
    fi
}

# ─── Auto-get RDS Endpoint from Terraform state ─────────
derive_rds_endpoint() {
    if [[ -z "${RDS_ENDPOINT:-}" ]]; then
        log_info "Auto-deriving RDS Endpoint from Terraform state..."

        if [[ -f "${TERRAFORM_DIR}/terraform.tfstate" ]] || [[ -d "${TERRAFORM_DIR}/.terraform" ]]; then
            cd "$TERRAFORM_DIR"
            RDS_ENDPOINT=$(terraform output -raw rds_endpoint 2>/dev/null) || {
                log_error "Could not retrieve RDS endpoint from Terraform. Run Phase 4 first."
                cd ..
                exit 1
            }
            cd ..
            log_info "  RDS Endpoint: $RDS_ENDPOINT"
        else
            log_error "Terraform state not found. Complete Phase 4 (RDS setup) first."
            exit 1
        fi
    fi
}

# ─── Auto-create Cognito User Pool ──────────────────────
setup_cognito() {
    if [[ -z "${COGNITO_ISSUER_URI:-}" ]]; then
        log_info "Setting up AWS Cognito User Pool..."

        local pool_name="${ENVIRONMENT}-pool"

        # Check if pool already exists
        local existing_pool=$(aws cognito-idp list-user-pools --max-results 10 --region "$AWS_REGION" \
            --query "UserPools[?Name=='$pool_name'].Id" --output text 2>/dev/null)

        if [[ -n "$existing_pool" ]]; then
            log_info "  Using existing Cognito pool: $existing_pool"
            COGNITO_POOL_ID=$existing_pool
        else
            log_info "  Creating new Cognito User Pool: $pool_name"
            COGNITO_POOL_ID=$(aws cognito-idp create-user-pool \
                --pool-name "$pool_name" \
                --region "$AWS_REGION" \
                --policies 'PasswordPolicy={MinimumLength=8,RequireUppercase=false,RequireLowercase=false,RequireNumbers=false,RequireSymbols=false}' \
                --query 'UserPool.Id' \
                --output text) || {
                log_error "Failed to create Cognito User Pool"
                exit 1
            }
            log_info "  Created Cognito pool: $COGNITO_POOL_ID"
        fi

        COGNITO_ISSUER_URI="https://cognito-idp.${AWS_REGION}.amazonaws.com/${COGNITO_POOL_ID}"
        log_info "  Cognito Issuer URI: $COGNITO_ISSUER_URI"
    fi
}

# ─── Validate required variables ────────────────────────
validate_config() {
    local required_vars=(
        "RDS_PASSWORD"
    )

    local missing_vars=()
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var:-}" ]]; then
            missing_vars+=("$var")
        fi
    done

    if [[ ${#missing_vars[@]} -gt 0 ]]; then
        log_error "Missing required configuration variables:"
        printf '  - %s\n' "${missing_vars[@]}"
        exit 1
    fi

    log_success "Configuration validated"
}

# ─── Create/update SSM Parameters ──────────────────────
setup_ssm_parameters() {
    log_info "Setting up AWS SSM Parameter Store..."

    # Parameters: name|value|type (pipe-delimited to avoid issues with colons in values)
    # Using simple names without paths to avoid IAM permission issues
    # Note: RDS_ENDPOINT already includes port (hostname:5432), so don't add it again
    local params=(
        "rds-db-url|jdbc:postgresql://${RDS_ENDPOINT}/dynamicform|String"
        "rds-db-username|${RDS_USERNAME}|String"
        "rds-db-password|${RDS_PASSWORD}|SecureString"
        "oauth2-issuer-uri|${COGNITO_ISSUER_URI}|String"
    )

    for param in "${params[@]}"; do
        # Split on pipe character instead of colon (more reliable)
        IFS='|' read -r name value type <<< "$param"

        log_info "  Creating parameter: $name"
        if ! aws ssm put-parameter \
            --name "$name" \
            --value "$value" \
            --type "$type" \
            --region "$AWS_REGION" \
            --overwrite \
            --no-cli-pager 2>&1; then
            log_error "Failed to create SSM parameter: $name"
            log_error "Check your AWS credentials and IAM permissions:"
            log_error "  1. Verify identity: aws sts get-caller-identity"
            log_error "  2. IAM user needs: ssm:PutParameter"
            log_error "  3. Check policy: aws iam list-user-policies --user-name <your-user>"
            exit 1
        fi
    done

    log_success "SSM parameters created/updated"
}

# ─── Deploy infrastructure with Terraform ──────────────
deploy_terraform() {
    log_info "Deploying infrastructure with Terraform..."

    cd "$TERRAFORM_DIR"

    log_info "  Running: terraform init"
    terraform init -no-color > /dev/null 2>&1 || {
        log_error "Terraform init failed"
        exit 1
    }

    log_info "  Running: terraform plan"
    terraform plan -no-color > /tmp/tf-plan.txt 2>&1 || {
        log_error "Terraform plan failed"
        cat /tmp/tf-plan.txt
        exit 1
    }

    if [[ "$TERRAFORM_AUTO_APPROVE" == "true" ]]; then
        log_info "  Running: terraform apply (auto-approved)"
        if ! terraform apply -no-color -auto-approve \
            -var="create_test_users=$CREATE_TEST_USERS" 2>&1; then
            log_error "Terraform apply failed"
            log_error "Review the error above and troubleshoot"
            exit 1
        fi
    else
        log_warning "Review the plan above and run: terraform apply"
        exit 0
    fi

    cd ..
    log_success "Infrastructure deployed successfully"
}

# ─── Capture Terraform outputs ──────────────────────────
capture_terraform_outputs() {
    log_info "Capturing Terraform outputs..."

    cd "$TERRAFORM_DIR"

    ECR_REPOSITORY_URL=$(terraform output -raw ecr_repository_url)
    ALB_DNS_NAME=$(terraform output -raw alb_dns_name)
    ECS_CLUSTER_NAME=$(terraform output -raw ecs_cluster_name)
    ECS_SERVICE_NAME=$(terraform output -raw ecs_service_name)

    cd ..

    log_success "Terraform outputs captured"
    log_info "  ECR Repository: $ECR_REPOSITORY_URL"
    log_info "  ALB DNS: $ALB_DNS_NAME"
    log_info "  ECS Cluster: $ECS_CLUSTER_NAME"
    log_info "  ECS Service: $ECS_SERVICE_NAME"
}

# ─── Build and push Docker image ────────────────────────
build_and_push_image() {
    log_info "Building and pushing Docker image to ECR..."

    log_info "  Authenticating with ECR"
    aws ecr get-login-password --region "$AWS_REGION" | \
        docker login --username AWS --password-stdin \
        "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com" \
        > /dev/null 2>&1 || {
        log_error "ECR login failed"
        exit 1
    }

    log_info "  Building image with Jib (this may take 2-3 minutes)"
    mvn -pl frontend install -DskipTests
    mvn -pl backend install jib:build -DskipTests \
        -Djib.to.image="${ECR_REPOSITORY_URL}:latest" \
        -Dorg.slf4j.simpleLogger.defaultLogLevel=warn \
        > /tmp/jib-build.log 2>&1 || {
        log_error "Docker image build failed"
        tail -20 /tmp/jib-build.log
        exit 1
    }

    log_success "Docker image built and pushed"
}

# ─── Redeploy ECS service ──────────────────────────────
redeploy_ecs() {
    log_info "Triggering ECS service redeployment..."

    aws ecs update-service \
        --cluster "$ECS_CLUSTER_NAME" \
        --service "$ECS_SERVICE_NAME" \
        --force-new-deployment \
        --region "$AWS_REGION" \
        --no-cli-pager \
        > /dev/null 2>&1 || {
        log_error "ECS update-service failed"
        exit 1
    }

    log_success "ECS service redeployment triggered"
}

# ─── Verify health check ────────────────────────────────
verify_health() {
    log_info "Waiting for service to stabilize ($HEALTH_CHECK_WAIT seconds)..."
    sleep "$HEALTH_CHECK_WAIT"

    log_info "Checking health endpoint..."
    local elapsed=0
    local health_check_url="http://${ALB_DNS_NAME}${HEALTH_CHECK_ENDPOINT}"

    while [[ $elapsed -lt $HEALTH_CHECK_TIMEOUT ]]; do
        if curl -sf "$health_check_url" > /dev/null 2>&1; then
            log_success "Health check passed"
            log_info "Application is running at: http://${ALB_DNS_NAME}"
            return 0
        fi

        log_warning "Health check failed, retrying... ($elapsed/$HEALTH_CHECK_TIMEOUT seconds)"
        sleep 5
        elapsed=$((elapsed + 5))
    done

    log_error "Health check failed after ${HEALTH_CHECK_TIMEOUT}s"
    log_info "Check ECS task logs: aws logs tail /ecs/${ENVIRONMENT}-app --follow"
    exit 1
}

# ─── Main execution ─────────────────────────────────────
main() {
    log_info "Starting AWS deployment pipeline..."
    echo

    # Auto-derive/setup missing configuration
    derive_aws_account_id
    derive_rds_endpoint
    setup_cognito

    echo
    log_info "Configuration Summary:"
    log_info "  AWS Region: $AWS_REGION"
    log_info "  AWS Account ID: $AWS_ACCOUNT_ID"
    log_info "  RDS Endpoint: $RDS_ENDPOINT"
    log_info "  Cognito Issuer URI: $COGNITO_ISSUER_URI"
    log_info "  Environment: $ENVIRONMENT"
    echo

    validate_config
    setup_ssm_parameters
    deploy_terraform
    capture_terraform_outputs
    build_and_push_image
    redeploy_ecs
    verify_health

    echo
    log_success "Deployment completed successfully!"
    log_info "Application available at: http://${ALB_DNS_NAME}"
}

# Run main function
main "$@"
