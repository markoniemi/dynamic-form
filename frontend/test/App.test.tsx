import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';
import App from '../src/App';

const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

describe('App', () => {
  beforeEach(() => {
    // Suppress console errors during tests
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('loads OIDC config from /api/config/oauth2-issuer-uri on mount', async () => {
    const queryClient = createTestQueryClient();

    render(
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith('/api/config/oauth2-issuer-uri');
    });

    // Verify the app renders with loaded config (check for nav or content)
    await waitFor(() => {
      expect(screen.getByRole('navigation')).toBeInTheDocument();
    });
  });
});
