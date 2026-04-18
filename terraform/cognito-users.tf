# Optional Cognito pool and test users for testing
# Set create_test_users = true to enable test user creation

variable "cognito_user_pool_id" {
  description = "Cognito User Pool ID (from setup.sh or AWS console)"
  type        = string
  default     = ""
}

locals {
  test_users = var.create_test_users ? {
    admin = {
      username = "admin"
      email    = "admin@example.com"
      password = "admin"
      role     = "ROLE_ADMIN"
    }
    user = {
      username = "user"
      email    = "user@example.com"
      password = "user"
      role     = "ROLE_USER"
    }
  } : {}
}

# Note: Cognito User Pool is created by setup.sh via AWS CLI
# This data block would reference it, but test users are also created via AWS CLI

# Create Cognito groups for roles
resource "aws_cognito_user_group" "roles" {
  for_each = var.create_test_users && var.cognito_user_pool_id != "" ? toset(["ROLE_ADMIN", "ROLE_USER"]) : toset([])

  name            = each.value
  user_pool_id    = var.cognito_user_pool_id
  description     = "Role group ${each.value}"
  precedence      = each.value == "ROLE_ADMIN" ? 1 : 2
}

# Note: Test users are created by setup.sh via AWS CLI (aws cognito-idp admin-create-user)
# These terraform resources are not used because the resource types may not be available
# in all terraform AWS provider versions. AWS CLI approach is more reliable.
