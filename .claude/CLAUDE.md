# Claude Development Guidelines

This document provides development guidelines for this project. For detailed Copilot and code generation instructions, please refer to the GitHub Copilot Instructions.

## Quick Reference

- **Copilot Instructions**: See [.github/copilot-instructions.md](../.github/copilot-instructions.md) for comprehensive guidance on coding conventions, architecture patterns, and development standards.

## Key Resources

- **TechnicalSpecification.md**: Defines the technical architecture and stack
- **RequirementsSpecification.md**: Outlines project requirements and features
- **copilot-instructions.md**: Complete coding standards and guidelines for all team members

## Architecture Overview

This is a monolithic Spring Boot application with:
- **Backend**: Java/Spring Boot REST API
- **Frontend**: React/TypeScript SPA packaged as a WebJar
- **Database**: PostgreSQL with H2 for tests
- **Authentication**: OAuth 2.0 with Spring Security
- **Error Handling**: RFC 7807 Problem Details with validation error extensions

### Key Architectural Patterns

**Error Responses** — All API errors follow RFC 7807 (`ProblemDetail`). Validation errors include an `errors` array with field-level details (`{ field, message, code }`). Frontend wires these directly into form field errors using react-hook-form.

**Validation** — Spring Validation Framework (`@Valid`, `@NotBlank`, etc.) on DTOs. Global exception handler extracts field errors and returns them in the `errors` extension of `ProblemDetail`.

For detailed information about coding conventions, architecture patterns, and development practices, refer to the [Copilot Instructions](../.github/copilot-instructions.md).
