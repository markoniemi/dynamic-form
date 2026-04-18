#!/bin/bash

################################################################################
# AWS & GitHub Setup Script — One-time Initial Configuration
#
# Usage:
#   bash setup.sh
#
# This script performs one-time setup:
#   1. Verifies AWS credentials (admin access required)
#   2. Creates IAM user for GitHub Actions (github-actions-deploy)
#   3. Creates and attaches IAM policy with least-privilege permissions
#   4. Generates access keys for the IAM user
#   5. Creates Cognito User Pool for OAuth2
#   6. Prompts for GitHub token and sets GitHub secrets
#
# After this script completes, you can run: bash deploy.sh <password>
#
# Prerequisites:
#   - AWS CLI configured with admin credentials (aws configure)
#   - GitHub CLI (gh) installed and authenticated
#   - You have admin access to the GitHub repository
################################################################################

set -euo pipefail

# ─── Color output ───────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
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

log_step() {
    echo
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}$*${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# ─── Configuration ──────────────────────────────────────
AWS_REGION="eu-north-1"
IAM_USER_NAME="github-actions-deploy"
IAM_POLICY_NAME="dynamic-form-deploy-policy"
COGNITO_POOL_NAME="dynamic-form-pool"
TERRAFORM_DIR="terraform"

# ─── Check prerequisites ────────────────────────────────
check_prerequisites() {
    log_step "Checking prerequisites"

    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI not found. Install it: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html"
        exit 1
    fi
    log_success "AWS CLI found"

    # Check GitHub CLI
    if ! command -v gh &> /dev/null; then
        log_error "GitHub CLI not found. Install it: https://cli.github.com/"
        exit 1
    fi
    log_success "GitHub CLI found"

    # Check AWS credentials
    if ! aws sts get-caller-identity > /dev/null 2>&1; then
        log_error "AWS credentials not configured. Run: aws configure"
        exit 1
    fi
    log_success "AWS credentials configured"

    # Check GitHub authentication
    if ! gh auth status > /dev/null 2>&1; then
        log_error "GitHub CLI not authenticated. Run: gh auth login"
        exit 1
    fi
    log_success "GitHub CLI authenticated"
}

# ─── Get AWS account details ────────────────────────────
get_aws_details() {
    log_step "Getting AWS account details"

    AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    CURRENT_USER=$(aws sts get-caller-identity --query Arn --output text)

    log_info "AWS Account ID: $AWS_ACCOUNT_ID"
    log_info "Current user: $CURRENT_USER"
    log_success "AWS details retrieved"
}

# ─── Check if IAM user already exists ───────────────────
check_iam_user() {
    log_step "Checking if IAM user exists"

    if aws iam get-user --user-name "$IAM_USER_NAME" > /dev/null 2>&1; then
        log_warning "IAM user '$IAM_USER_NAME' already exists"
        read -p "Delete and recreate it? (y/n): " recreate
        if [[ "$recreate" == "y" ]]; then
            log_info "Deleting existing IAM user and policies..."
            aws iam delete-access-key --user-name "$IAM_USER_NAME" --access-key-id "$(aws iam list-access-keys --user-name "$IAM_USER_NAME" --query 'AccessKeyMetadata[0].AccessKeyId' --output text)" 2>/dev/null || true
            aws iam delete-user-policy --user-name "$IAM_USER_NAME" --policy-name "$IAM_POLICY_NAME" 2>/dev/null || true
            aws iam delete-user --user-name "$IAM_USER_NAME"
            log_success "Existing IAM user deleted"
        else
            log_info "Using existing IAM user"
            return 1
        fi
    fi
    return 0
}

# ─── Create IAM user ────────────────────────────────────
create_iam_user() {
    log_step "Creating IAM user for GitHub Actions"

    if ! check_iam_user; then
        log_warning "Skipping IAM user creation (already exists)"
        return
    fi

    log_info "Creating user: $IAM_USER_NAME"
    aws iam create-user --user-name "$IAM_USER_NAME" > /dev/null || {
        log_error "Failed to create IAM user"
        exit 1
    }
    log_success "IAM user created"
}

# ─── Attach IAM policy ──────────────────────────────────
attach_iam_policy() {
    log_step "Attaching IAM policy"

    log_info "Reading policy from: docs/deploy-policy.json"
    if [[ ! -f "docs/deploy-policy.json" ]]; then
        log_error "Policy file not found: docs/deploy-policy.json"
        exit 1
    fi

    log_info "Attaching policy: $IAM_POLICY_NAME"
    aws iam put-user-policy \
        --user-name "$IAM_USER_NAME" \
        --policy-name "$IAM_POLICY_NAME" \
        --policy-document "file://docs/deploy-policy.json" > /dev/null || {
        log_error "Failed to attach IAM policy"
        exit 1
    }
    log_success "IAM policy attached"
}

# ─── Generate access keys ───────────────────────────────
generate_access_keys() {
    log_step "Generating access keys for GitHub Actions"

    log_info "Creating access key..."
    # AWS text output format: ACCESSKEY <AccessKeyId> <CreateDate> <SecretAccessKey> <Status> <UserName>
    local keys=$(aws iam create-access-key --user-name "$IAM_USER_NAME" --output text)

    AWS_ACCESS_KEY_ID=$(echo "$keys" | awk '{print $2}')
    AWS_SECRET_ACCESS_KEY=$(echo "$keys" | awk '{print $4}')

    log_success "Access keys generated"
    log_warning "SAVE THESE IMMEDIATELY — Secret access key shown only once:"
    echo
    echo "  Access Key ID:     $AWS_ACCESS_KEY_ID"
    echo "  Secret Access Key: $AWS_SECRET_ACCESS_KEY"
    echo
}

# ─── Setup RDS Database ────────────────────────────────
setup_rds() {
    log_step "Setting up RDS PostgreSQL Database"

    read -sp "Enter RDS master password (will be used for terraform): " RDS_PASSWORD
    echo

    if [[ -z "$RDS_PASSWORD" ]]; then
        log_error "RDS password cannot be empty"
        exit 1
    fi

    log_info "Creating terraform.tfvars with RDS configuration..."
    cat > "${TERRAFORM_DIR}/terraform.tfvars" <<EOF
db_username = "postgres"
db_password = "$RDS_PASSWORD"
EOF
    log_success "terraform.tfvars created"

    log_info "Initializing Terraform..."
    cd "$TERRAFORM_DIR"
    terraform init > /dev/null 2>&1 || {
        log_error "Terraform init failed"
        exit 1
    }
    log_success "Terraform initialized"

    log_info "Creating infrastructure (VPC, subnets, RDS instance - this may take 10-15 minutes)..."
    if terraform apply -auto-approve > /tmp/tf-apply.log 2>&1; then
        log_success "Infrastructure created successfully"
    else
        log_error "Infrastructure creation failed"
        tail -30 /tmp/tf-apply.log
        exit 1
    fi

    RDS_ENDPOINT=$(terraform output -raw rds_endpoint 2>/dev/null)
    if [[ -z "$RDS_ENDPOINT" ]]; then
        log_error "Could not retrieve RDS endpoint"
        exit 1
    fi

    cd ..
    log_success "RDS Endpoint: $RDS_ENDPOINT"
    log_info "RDS Password: (saved in terraform/terraform.tfvars)"
}

# ─── Setup Cognito User Pool ────────────────────────────
setup_cognito() {
    log_step "Setting up Cognito User Pool"

    # Check if pool already exists
    local existing_pool=$(aws cognito-idp list-user-pools --max-results 10 --region "$AWS_REGION" \
        --query "UserPools[?Name=='$COGNITO_POOL_NAME'].Id" --output text 2>/dev/null)

    if [[ -n "$existing_pool" ]]; then
        log_warning "Cognito pool already exists: $existing_pool"
        COGNITO_POOL_ID=$existing_pool
    else
        log_info "Creating Cognito User Pool: $COGNITO_POOL_NAME"
        COGNITO_POOL_ID=$(aws cognito-idp create-user-pool \
            --pool-name "$COGNITO_POOL_NAME" \
            --region "$AWS_REGION" \
            --policies 'PasswordPolicy={MinimumLength=8,RequireUppercase=false,RequireLowercase=false,RequireNumbers=false,RequireSymbols=false}' \
            --query 'UserPool.Id' \
            --output text) || {
            log_error "Failed to create Cognito User Pool"
            exit 1
        }
        log_success "Cognito User Pool created: $COGNITO_POOL_ID"
    fi

    COGNITO_ISSUER_URI="https://cognito-idp.${AWS_REGION}.amazonaws.com/${COGNITO_POOL_ID}"
    log_info "Cognito Issuer URI: $COGNITO_ISSUER_URI"

    # Create test users
    create_cognito_test_users
}

# ─── Create Cognito test users ──────────────────────────
create_cognito_test_users() {
    log_step "Creating Cognito test users"

    # Define test users: username, password, email, role
    declare -A test_users=(
        [admin]="admin admin@example.com ROLE_ADMIN"
        [user]="user user@example.com ROLE_USER"
    )

    # Create groups first
    log_info "Creating Cognito groups..."
    for group in ROLE_ADMIN ROLE_USER; do
        if aws cognito-idp get-group --user-pool-id "$COGNITO_POOL_ID" --group-name "$group" \
            --region "$AWS_REGION" > /dev/null 2>&1; then
            log_info "  Group already exists: $group"
        else
            aws cognito-idp create-group \
                --user-pool-id "$COGNITO_POOL_ID" \
                --group-name "$group" \
                --description "Role group $group" \
                --region "$AWS_REGION" > /dev/null 2>&1 && \
                log_success "  Created group: $group" || \
                log_warning "  Failed to create group: $group"
        fi
    done

    # Create users
    log_info "Creating test users..."
    for username in "${!test_users[@]}"; do
        read -r password email role <<< "${test_users[$username]}"

        if aws cognito-idp admin-get-user --user-pool-id "$COGNITO_POOL_ID" --username "$username" \
            --region "$AWS_REGION" > /dev/null 2>&1; then
            log_warning "  User already exists: $username"
        else
            aws cognito-idp admin-create-user \
                --user-pool-id "$COGNITO_POOL_ID" \
                --username "$username" \
                --temporary-password "$password" \
                --message-action SUPPRESS \
                --user-attributes "Name=email,Value=$email" "Name=email_verified,Value=true" \
                --region "$AWS_REGION" > /dev/null 2>&1 || {
                log_error "Failed to create user: $username"
                continue
            }

            # Set permanent password
            aws cognito-idp admin-set-user-password \
                --user-pool-id "$COGNITO_POOL_ID" \
                --username "$username" \
                --password "$password" \
                --permanent \
                --region "$AWS_REGION" > /dev/null 2>&1 || {
                log_error "Failed to set password for user: $username"
                continue
            }

            log_success "  Created user: $username (password: $password)"

            # Add user to group
            aws cognito-idp admin-add-user-to-group \
                --user-pool-id "$COGNITO_POOL_ID" \
                --username "$username" \
                --group-name "$role" \
                --region "$AWS_REGION" > /dev/null 2>&1 || {
                log_error "Failed to add user to group: $username -> $role"
            }
        fi
    done

    log_success "Test users configured"
}

# ─── Set GitHub secrets ─────────────────────────────────
set_github_secrets() {
    log_step "Setting GitHub secrets"

    log_info "Getting GitHub repository..."
    local repo=$(gh repo view --json nameWithOwner --query nameWithOwner --template '{{.}}')
    log_info "Repository: $repo"

    log_info "Setting secrets..."

    gh secret set AWS_ACCESS_KEY_ID --body "$AWS_ACCESS_KEY_ID" --repo "$repo" 2>/dev/null && \
        log_success "  AWS_ACCESS_KEY_ID set" || \
        log_error "Failed to set AWS_ACCESS_KEY_ID"

    gh secret set AWS_SECRET_ACCESS_KEY --body "$AWS_SECRET_ACCESS_KEY" --repo "$repo" 2>/dev/null && \
        log_success "  AWS_SECRET_ACCESS_KEY set" || \
        log_error "Failed to set AWS_SECRET_ACCESS_KEY"

    gh secret set AWS_REGION --body "$AWS_REGION" --repo "$repo" 2>/dev/null && \
        log_success "  AWS_REGION set" || \
        log_error "Failed to set AWS_REGION"

    gh secret set AWS_ACCOUNT_ID --body "$AWS_ACCOUNT_ID" --repo "$repo" 2>/dev/null && \
        log_success "  AWS_ACCOUNT_ID set" || \
        log_error "Failed to set AWS_ACCOUNT_ID"

    gh secret set ECR_REPOSITORY --body "dynamic-form" --repo "$repo" 2>/dev/null && \
        log_success "  ECR_REPOSITORY set" || \
        log_error "Failed to set ECR_REPOSITORY"

    log_success "GitHub secrets configured"
}

# ─── Summary ────────────────────────────────────────────
print_summary() {
    log_step "Setup Complete!"

    echo
    echo "✅ All prerequisites configured:"
    echo
    echo "AWS Infrastructure:"
    echo "  • IAM User: $IAM_USER_NAME"
    echo "  • AWS Account ID: $AWS_ACCOUNT_ID"
    echo "  • AWS Region: $AWS_REGION"
    echo
    echo "Database:"
    echo "  • RDS Endpoint: $RDS_ENDPOINT"
    echo "  • RDS Username: postgres"
    echo "  • RDS Password: (saved in terraform/terraform.tfvars)"
    echo
    echo "Authentication:"
    echo "  • Cognito Pool ID: $COGNITO_POOL_ID"
    echo "  • Cognito Issuer URI: $COGNITO_ISSUER_URI"
    echo
    echo "Test Users (for OAuth2 testing):"
    echo "  • admin / admin (ROLE_ADMIN)"
    echo "  • user / user (ROLE_USER)"
    echo
    echo "GitHub Secrets:"
    echo "  • AWS_ACCESS_KEY_ID"
    echo "  • AWS_SECRET_ACCESS_KEY"
    echo "  • AWS_REGION"
    echo "  • AWS_ACCOUNT_ID"
    echo "  • ECR_REPOSITORY"
    echo
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo
    echo "Next steps:"
    echo "  1. Complete Phase 0 (application configuration)"
    echo "  2. Run the deployment:"
    echo
    echo "     bash deploy.sh postgres"
    echo
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo
}

# ─── Main execution ─────────────────────────────────────
main() {
    log_info "AWS & GitHub Setup Script"
    log_warning "This is a one-time setup. Ensure you have admin AWS credentials!"
    echo

    check_prerequisites
    get_aws_details
    create_iam_user
    attach_iam_policy
    generate_access_keys
    setup_rds
    setup_cognito
    set_github_secrets
    print_summary
}

# Run main function
main "$@"
