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

For detailed information about coding conventions, architecture patterns, and development practices, refer to the [Copilot Instructions](../.github/copilot-instructions.md).
