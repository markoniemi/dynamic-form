# GitHub Copilot Instructions for the Form Application

This document provides guidance for GitHub Copilot to assist in the development of this project, ensuring that generated code aligns with the architecture and technologies outlined in the `TechnicalSpecification.md`.

## General Principles

*   **Monolithic Architecture**: Remember that this is a monolithic application. The Spring Boot backend serves both the REST API and the React frontend. The frontend is packaged as a WebJar and included as a dependency in the backend.
*   **Follow Existing Patterns**: When generating code, please analyze the existing files to match the coding style, naming conventions, and architectural patterns.
*   **Adhere to Specifications**: All generated code should align with the `TechnicalSpecification.md` and `RequirementsSpecification.md`.

## Coding Conventions

### Java (Backend)
*   **Style Guide**: Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
*   **Formatting**: Use Google Java Format for consistent code formatting. Use two spaces for tabs.
*   **Naming**: Use clear and descriptive names for classes, methods, and variables (e.g., `FormService`, `getFormById`).
*   **Comments**: Use javadoc sparingly, only for external IDE and for complex structures or unusual implementations.
*   **Immutability**: Prefer immutability where possible, especially for DTOs and configuration properties. Use @Value annotation where possible.
* Use lombok whenever possible.
* Use constructor injection.
*   **Follow the Single Responsibility Principle**: Each class or method should have one, and only one, reason to change.
*   **Meaningful Names**: Use clear, intention-revealing names for variables, methods, and classes.
*   **Keep Methods Small**: Methods should be small and do one thing. If a method is doing too many things, break it down into smaller, more focused methods.
*   **Comments are a Last Resort**: Code should be self-documenting. If you need to add a comment, consider refactoring the code to make it clearer.
*   **Don't Repeat Yourself (DRY)**: Avoid duplicating code. Use abstraction to reuse common logic.
*   **Favor Polymorphism over If/Else or Switch/Case**: When dealing with different behaviors based on type, use polymorphism to create more maintainable and scalable code.


### TypeScript/React (Frontend)
*   **Style Guide**: Follow the [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript) and the official TypeScript guidelines.
*   **Naming**:
    *   Use `PascalCase` for React components (e.g., `FormBuilderPage`) and classes (e.g., `FormService`).
    *   Use `camelCase` for variables, functions, and hooks (e.g., `useForm`, `fetchFormData`).
    * A base filename should exactly match the name of its default export.
*   **Components**: Always use functional components with hooks. Avoid class components.
*   **Variables**: Use `const` by default; use `let` only when a variable needs to be reassigned.
* Use useQuery and useMutation from react-query for data fetching and mutations.
* **Comments**: Use comments sparingly to explain complex logic, but avoid obvious comments.
* **Type Safety**: Always define types and interfaces for props, state, and API responses.
* **Styling**: Prefer using `react-bootstrap` components and utility classes over custom CSS.
* **File Organization**: Organize files by feature or domain rather than by type (e.g., group related components, hooks, and services together).
* **Avoid any**: Do not use the `any` type; always strive for precise typing.
* **Error Handling**: Implement proper error handling for asynchronous operations, using try/catch blocks or error boundaries as appropriate.
* **Testing**: Write unit tests for components and hooks using React Testing Library and Vitest.
* **Accessibility**: Follow accessibility best practices, including using semantic HTML and ARIA attributes where necessary.
* **State Management**: Use React's built-in state management (useState, useContext).
* **API Integration**: Use Fetch API for making HTTP requests, and handle loading and error states appropriately. Use useQuery for api calls. 
* **Validation**: Use Zod and react hook form for validation.
* **Dependencies**: Keep dependencies up to date and avoid unnecessary packages to minimize bundle size.

## Backend (Java & Spring Boot)

The backend is a standard Spring Boot application.

*   **Stack**:
    *   **Language**: Java
    *   **Framework**: Spring Boot (using Spring Web, Spring Data JPA, Spring Security)
    *   **Database**: PostgreSQL, H2 (for tests)
    *   **Authentication**: OAuth 2.0 with Spring Security and a development-time Spring Boot Authorization Server.

*   **Instructions**:
    *   **Entities**: Use standard JPA annotations (`@Entity`, `@Table`, `@Id`, `@ManyToOne`, etc.).
    *   **Repositories**: For each entity, create a corresponding Spring Data JPA repository. It should extend `JpaRepository`.
    *   **Services**: Business logic should be placed in service. Use `@Service` and inject repositories.
    *   **Controllers**: Create REST controllers to expose API endpoints as defined in the technical specification. Use `@RestController` and `@RequestMapping("/api/...")`. Secure endpoints using Spring Security annotations (`@PreAuthorize`).
    *   **Testing**:
        *   For unit tests, use JUnit 5 and Mockito.
        *   For integration tests involving the database, use Spring Boot's test slices (`@DataJpaTest`) with an in-memory H2 database.
        *   For integration tests involving security, use `@SpringBootTest` and Testcontainers to provide an OAuth 2.0 authorization server.
        * Use JUnit5 asserts.
        * Use following pattern for test method names:
          * `MethodUnderTest`
            * example: withdrawCash
          * `MethodUnderTest`With`StateUnderTest``ExpectedBehavior`
            * example: withdrawCashWithInsufficientBalanceThrowsException

## Frontend (TypeScript & React)

The frontend is a single-page application built with React and TypeScript.

*   **Stack**:
    *   **Language**: TypeScript
    *   **Framework**: React
    *   **Styling**: Bootstrap (via `react-bootstrap` components)
    *   **Build**: The project is located in the `frontend` module and is built by the `frontend-maven-plugin`.

*   **Instructions**:
    *   **Directory Structure**: Follow a standard React application structure within `frontend/src/`:
        *   `components/`: Reusable UI components.
            *   `common/`: Generic components like buttons, inputs, etc.
            *   `layout/`: Components that define the page structure (e.g., Header, Footer).
        *   `pages/`: Top-level components that correspond to a page/route (e.g., `HomePage`, `FormBuilderPage`).
        *   `hooks/`: Custom React hooks.
        *   `services/`: Functions for interacting with the backend API.
        *   `types/`: Shared TypeScript types and interfaces.
        *   `styles/`: Global styles or theme files.
    *   **Components**: Create functional components using React Hooks. Use `.tsx` for files containing JSX.
    *   **Styling**: Use `react-bootstrap` components (e.g., `<Button>`, `<Form>`, `<Container>`) for the UI. Avoid writing custom CSS where a Bootstrap component or utility class can be used.
    *   **State Management**: For simple component state, use `useState`. For more complex global state, consider using `useContext` with `useReducer`.
    *   **API Interaction**: Place API interaction logic in the `services/` directory. Use fetch for making requests. Use useRequest hooks in components.
    *   **Authentication**: The frontend should handle the OAuth 2.0 redirect flow. When making authenticated API calls, ensure the request includes the necessary credentials.

## Agent Profiles

The following agent profiles define specialized personas for different development tasks. Reference these when working on specific areas of the application.

**Default Agent: Full Stack Developer** - Unless otherwise specified, act as the Full Stack Developer Agent.

### Backend Developer Agent
When working on backend tasks, focus on:
*   **Primary Responsibilities:**
    *   REST API design and implementation following OpenAPI specifications
    *   JPA entity design and database schema alignment
    *   Service layer business logic implementation
    *   Spring Security configuration and endpoint protection
    *   Unit and integration testing with JUnit 5 and Mockito
*   **Key Priorities:**
    1.  Ensure data integrity and proper validation using Bean Validation annotations (`@NotNull`, `@Size`, `@Valid`)
    2.  Implement proper exception handling with `@ControllerAdvice` or `@RestControllerAdvice` and custom exceptions
    3.  Use DTOs for API request/response objects, never expose entities directly
    4.  Use mapstruct for entity-DTO mapping
    5.  Include comprehensive logging with SLF4J
    6.  Write tests for all new functionality

### Frontend Developer Agent
When working on frontend tasks, focus on:
*   **Primary Responsibilities:**
    *   React component design using functional components and hooks
    *   User interface implementation with react-bootstrap
    *   State management with useState, useContext, and useReducer
    *   API integration via services layer
    *   Responsive and accessible UI design
*   **Key Priorities:**
    1.  Create reusable, well-typed components
    2.  Ensure proper form validation and user feedback
    3.  Handle loading states and error conditions gracefully
    4.  Follow accessibility best practices (ARIA labels, keyboard navigation)
    5.  Maintain consistent styling with Bootstrap components

### Full Stack Developer Agent
When working on features that span both frontend and backend:
*   **Primary Responsibilities:**
    *   End-to-end feature implementation
    *   API contract design between frontend and backend
    *   Integration testing across layers
    *   Ensuring consistent data models (DTOs match TypeScript types)
*   **Key Priorities:**
    1.  Design API endpoints before implementation
    2.  Create matching TypeScript interfaces for all DTOs
    3.  Implement backend first, then frontend
    4.  Test the complete flow from UI to database

### QA/Testing Agent
When working on testing tasks, focus on:
*   **Primary Responsibilities:**
    *   Unit test implementation with JUnit 5 and Mockito
    *   Integration tests with `@DataJpaTest` and `@SpringBootTest`
    *   Frontend component testing
    *   API endpoint testing
*   **Key Priorities:**
    1.  Achieve high code coverage for business logic
    2.  Test edge cases and error conditions
    3.  Use Testcontainers for database integration tests
    4.  Mock external dependencies appropriately

### DevOps Agent
When working on infrastructure and deployment tasks:
*   **Primary Responsibilities:**
    *   Docker configuration and docker-compose setup
    *   Build pipeline configuration
    *   Environment-specific configuration (dev, staging, prod)
*   **Key Priorities:**
    1.  Maintain consistent environments across development and production
    2.  Ensure database scripts are idempotent
    3.  Configure proper health checks and monitoring
    4.  Manage secrets and environment variables securely
