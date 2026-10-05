import React from 'react';
import {render} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter, Route, Routes} from 'react-router-dom';

export function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {retry: false},
      mutations: {retry: false},
    },
  });
}

interface RenderOptions {
  /** Initial URL, e.g. '/forms/contact'. */
  route?: string;
  /** Route pattern matched against route so useParams works, e.g. '/forms/:formKey'. */
  path?: string;
}

/** Renders ui inside a fresh QueryClient and a MemoryRouter. */
export function renderWithProviders(ui: React.ReactElement, {route = '/', path}: RenderOptions = {}) {
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter initialEntries={[route]}>
        {path ? (
          <Routes>
            <Route path={path} element={ui}/>
          </Routes>
        ) : ui}
      </MemoryRouter>
    </QueryClientProvider>
  );
}
