import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import App from '../src/App';
import { createTestQueryClient } from './renderWithProviders';

describe('App', () => {
  beforeEach(() => {
    // Suppress console errors during tests
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('loads OIDC config from /api/config/oauth2-issuer-uri on mount', async () => {
    // App brings its own router, so only the QueryClient is provided here
    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <App />
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith('/api/config/oauth2-issuer-uri');
    });

    // Unauthenticated user is redirected to the login page
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'navigation.login' })).toBeInTheDocument();
    });
    expect(window.location.pathname).toBe('/login');
  });
});
