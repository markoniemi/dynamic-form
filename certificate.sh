#!/bin/bash

################################################################################
# SSL Certificate Generation & Import Script
#
# Usage:
#   bash certificate.sh <domain>
#
# Examples:
#   bash certificate.sh example.com           # Your custom domain
#   bash certificate.sh localhost             # For testing
#
# This script:
#   1. Generates a self-signed SSL certificate
#   2. Imports it to AWS Certificate Manager (ACM)
#   3. Outputs the certificate ARN for use in terraform
#
# For production, use AWS Certificate Manager with a real domain
#
# Prerequisites:
#   - AWS CLI configured with credentials
#   - OpenSSL installed (usually pre-installed)
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
DOMAIN="${1:-}"
AWS_REGION="${AWS_REGION:-eu-north-1}"
CERT_DIR="${PWD}/.certificates"
CERT_NAME="dynamic-form-${DOMAIN//./-}"
VALIDITY_DAYS=365

# ─── Check prerequisites ────────────────────────────────
check_prerequisites() {
    log_step "Checking prerequisites"

    if [[ -z "$DOMAIN" ]]; then
        log_error "Domain is required"
        log_error "Usage: bash certificate.sh <domain>"
        log_error "Examples:"
        log_error "  bash certificate.sh example.com"
        log_error "  bash certificate.sh localhost"
        exit 1
    fi

    # Check OpenSSL
    if ! command -v openssl &> /dev/null; then
        log_error "OpenSSL not found. Install it first:"
        log_error "  macOS: brew install openssl"
        log_error "  Ubuntu: sudo apt-get install openssl"
        log_error "  Windows: https://slproweb.com/products/Win32OpenSSL.html"
        exit 1
    fi
    log_success "OpenSSL installed"

    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI not found. Install it: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html"
        exit 1
    fi
    log_success "AWS CLI installed"

    # Check AWS credentials
    if ! aws sts get-caller-identity > /dev/null 2>&1; then
        log_error "AWS credentials not configured. Run: aws configure"
        exit 1
    fi
    log_success "AWS credentials configured"

    # Create certificate directory
    mkdir -p "$CERT_DIR"
    log_success "Certificate directory: $CERT_DIR"
}

# ─── Generate self-signed certificate ───────────────────
generate_certificate() {
    log_step "Generating self-signed SSL certificate for: $DOMAIN"

    local cert_file="${CERT_DIR}/${CERT_NAME}.crt"
    local key_file="${CERT_DIR}/${CERT_NAME}.key"

    # Check if certificate already exists
    if [[ -f "$cert_file" && -f "$key_file" ]]; then
        log_warning "Certificate already exists: $cert_file"
        read -p "Regenerate? (y/n): " regenerate
        if [[ "$regenerate" != "y" ]]; then
            log_info "Using existing certificate"
            return 0
        fi
    fi

    log_info "Generating certificate for: $DOMAIN"
    log_info "Validity: $VALIDITY_DAYS days"

    # Generate private key and self-signed certificate
    if openssl req -x509 \
        -newkey rsa:2048 \
        -keyout "$key_file" \
        -out "$cert_file" \
        -days "$VALIDITY_DAYS" \
        -nodes \
        -subj "/CN=$DOMAIN/O=Dynamic Form/C=US" 2>&1; then
        : # Success
    else
        # Fallback for Windows: use interactive mode
        log_warning "OpenSSL command failed, trying alternative method..."

        # Create a config file instead of using -subj
        cat > "${CERT_DIR}/openssl.conf" <<EOF
[req]
default_bits       = 2048
prompt             = no
default_md         = sha256
distinguished_name = req_distinguished_name

[req_distinguished_name]
C = US
O = Dynamic Form
CN = $DOMAIN
EOF

        if openssl req -x509 \
            -newkey rsa:2048 \
            -keyout "$key_file" \
            -out "$cert_file" \
            -days "$VALIDITY_DAYS" \
            -nodes \
            -config "${CERT_DIR}/openssl.conf" 2>&1; then
            : # Success
        else
            log_error "Failed to generate certificate"
            log_error "Please check that OpenSSL is correctly installed"
            exit 1
        fi
    fi

    log_success "Certificate generated: $cert_file"
    log_success "Private key generated: $key_file"

    # Display certificate info
    log_info "Certificate details:"
    openssl x509 -in "$cert_file" -text -noout | grep -E "Subject:|Issuer:|Not Before|Not After"
}

# ─── Import certificate to ACM ──────────────────────────
import_to_acm() {
    log_step "Importing certificate to AWS Certificate Manager"

    local cert_file="${CERT_DIR}/${CERT_NAME}.crt"
    local key_file="${CERT_DIR}/${CERT_NAME}.key"

    if [[ ! -f "$cert_file" || ! -f "$key_file" ]]; then
        log_error "Certificate files not found"
        exit 1
    fi

    log_info "Importing to ACM in region: $AWS_REGION"

    local cert_arn
    # Use MSYS_NO_PATHCONV only for AWS CLI to avoid path conversion issues
    cert_arn=$(MSYS_NO_PATHCONV=1 aws acm import-certificate \
        --certificate "fileb://${cert_file}" \
        --certificate-chain "fileb://${cert_file}" \
        --private-key "fileb://${key_file}" \
        --region "$AWS_REGION" \
        --query 'CertificateArn' \
        --output text) || {
        log_error "Failed to import certificate to ACM"
        log_error "Check that you have ACM permissions in your IAM policy"
        exit 1
    }

    if [[ -z "$cert_arn" ]]; then
        log_error "No certificate ARN returned from ACM"
        exit 1
    fi

    log_success "Certificate imported to ACM"
    log_success "Certificate ARN: $cert_arn"

    # Save ARN to file for later use
    echo "$cert_arn" > "${CERT_DIR}/${CERT_NAME}.arn"
    log_success "ARN saved to: ${CERT_DIR}/${CERT_NAME}.arn"
}

# ─── Print instructions ─────────────────────────────────
print_instructions() {
    log_step "Next Steps"

    local cert_arn
    cert_arn=$(cat "${CERT_DIR}/${CERT_NAME}.arn")

    echo
    echo "✅ Certificate ready for use with ALB"
    echo
    echo "Certificate ARN:"
    echo "  $cert_arn"
    echo
    echo "To use this certificate with your ALB:"
    echo
    echo "1. Update terraform/alb.tf with the certificate ARN:"
    echo "   Replace: certificate_arn = \"arn:aws:acm:...\""
    echo "   With:    certificate_arn = \"$cert_arn\""
    echo
    echo "2. Deploy:"
    echo "   bash deploy.sh postgres"
    echo
    echo "3. Then access your application:"
    echo "   https://<ALB_DNS_NAME>"
    echo
    echo "⚠️  Self-signed certificates generate browser warnings."
    echo "   For production, use AWS Certificate Manager with a real domain."
    echo
}

# ─── Main execution ─────────────────────────────────────
main() {
    log_info "SSL Certificate Generation Script"
    echo

    check_prerequisites
    generate_certificate
    import_to_acm
    print_instructions

    echo
    log_success "Certificate setup complete!"
}

# Run main function
main "$@"
