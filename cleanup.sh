#!/bin/bash

################################################################################
# AWS Cleanup Script — Complete Infrastructure Teardown
#
# Usage:
#   bash cleanup.sh              # Interactive (asks for confirmation)
#   bash cleanup.sh --force      # Non-interactive (destroys immediately)
#
# This script:
#   1. Destroys all Terraform infrastructure
#   2. Deletes SSM parameters
#   3. Deletes ECR images (keeps repository)
#   4. Cleans up local terraform state
#
# WARNING: This is destructive and cannot be undone!
################################################################################

set -euo pipefail

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
FORCE_MODE="${1:-}"
AWS_REGION="eu-north-1"
TERRAFORM_DIR="terraform"

# ─── Confirmation prompt ─────────────────────────────────
confirm_cleanup() {
    if [[ "$FORCE_MODE" == "--force" ]]; then
        return 0
    fi

    echo
    log_error "⚠️  WARNING: This will DELETE all AWS resources:"
    echo "    - ECS cluster, tasks, and services"
    echo "    - Application Load Balancer (ALB)"
    echo "    - RDS PostgreSQL database (DATA WILL BE LOST)"
    echo "    - VPC, subnets, and security groups"
    echo "    - ECR images"
    echo "    - CloudWatch logs"
    echo "    - SSM parameters"
    echo
    log_warning "This action CANNOT be undone!"
    echo
    read -p "Type 'yes' to confirm deletion: " confirmation

    if [[ "$confirmation" != "yes" ]]; then
        log_info "Cleanup cancelled"
        exit 0
    fi
}

# ─── Delete SSM Parameters ──────────────────────────────
delete_ssm_parameters() {
    log_info "Deleting SSM parameters..."

    local params=(
        "rds-db-url"
        "rds-db-username"
        "rds-db-password"
        "oauth2-issuer-uri"
    )

    for param in "${params[@]}"; do
        log_info "  Deleting parameter: $param"
        aws ssm delete-parameter --name "$param" --region "$AWS_REGION" 2>/dev/null || {
            log_warning "  Parameter not found: $param (skipping)"
        }
    done

    log_success "SSM parameters deleted"
}

# ─── Delete ECR Images ───────────────────────────────────
delete_ecr_images() {
    log_info "Clearing ECR repository..."

    local repo_name="dynamic-form"
    local images=$(aws ecr list-images --repository-name "$repo_name" --region "$AWS_REGION" --query 'imageIds[*]' --output json 2>/dev/null || echo "[]")

    if [[ "$images" != "[]" ]]; then
        log_info "  Deleting images from ECR repository"
        aws ecr batch-delete-image \
            --repository-name "$repo_name" \
            --image-ids "$images" \
            --region "$AWS_REGION" \
            > /dev/null 2>&1 || {
            log_warning "  Could not delete ECR images (repository may not exist)"
        }
    fi

    log_success "ECR images cleared (repository kept for reuse)"
}

# ─── Destroy Terraform Infrastructure ────────────────────
destroy_terraform() {
    log_info "Destroying Terraform infrastructure..."

    cd "$TERRAFORM_DIR"

    if [[ ! -f "terraform.tfstate" ]] && [[ ! -d ".terraform" ]]; then
        log_warning "No Terraform state found (infrastructure may not exist)"
        cd ..
        return 0
    fi

    log_info "  Running: terraform destroy (this may take 5-10 minutes)"
    if terraform destroy -auto-approve 2>&1; then
        log_success "Terraform infrastructure destroyed"
    else
        log_error "Terraform destroy failed"
        log_warning "Review the error above"
        cd ..
        return 1
    fi

    cd ..
}

# ─── Clean up local state ────────────────────────────────
cleanup_local_state() {
    log_info "Cleaning up local Terraform state..."

    cd "$TERRAFORM_DIR"

    # Keep the terraform directory but remove state files
    rm -f terraform.tfstate terraform.tfstate.backup terraform.tfvars.json .terraform.lock.hcl 2>/dev/null || true

    # Keep .terraform directory for provider cache (speeds up next deploy)
    # But you can uncomment below to delete it completely
    # rm -rf .terraform

    cd ..

    log_success "Local state cleaned up"
}

# ─── Main execution ─────────────────────────────────────
main() {
    log_info "AWS Infrastructure Cleanup Script"
    echo

    confirm_cleanup

    echo
    delete_ssm_parameters
    delete_ecr_images
    destroy_terraform
    cleanup_local_state

    echo
    log_success "Cleanup completed successfully!"
    log_info "You can now run 'bash deploy.sh postgres' to redeploy from scratch"
}

# Run main function
main "$@"
