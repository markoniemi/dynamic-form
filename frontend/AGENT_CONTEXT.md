# Frontend Module Context

## Overview
This module is the React frontend application built with Vite. It consumes APIs from the backend and interacts with the Auth Server for authentication.

## Key Technologies
- React 18
- TypeScript
- Vite
- `react-hook-form` + `zod` (Form management and validation)
- `@tanstack/react-query` (Server state management)
- `react-oidc-context` (OAuth2/OIDC client)
- `react-router-dom` (Routing)
- Bootstrap 5 + `react-bootstrap` (UI components)
- `vitest` + `@testing-library/react` (Testing)

## Configuration
- **Development Port**: 5173
- **API Proxy**: `/api/*` to `http://localhost:8080`
- **Auth Proxy**: `/oauth2/*` to `http://localhost:9000`

## Key Files
- `src/App.tsx`: Main application component and router setup.
- `src/main.tsx`: Entry point, sets up `QueryClientProvider` and `BrowserRouter`.
- `src/services/apiClient.ts`: Generic API client.
- `test/`: Vitest test files for components.
- `vitest.config.ts`: Vitest configuration.

## Development
- Install dependencies: `npm install`.
- Start dev server: `npm run dev`.
- Run tests: `npm test`.
