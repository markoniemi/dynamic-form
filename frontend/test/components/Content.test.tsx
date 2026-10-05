import {render, screen} from '@testing-library/react';
import {userEvent} from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {MemoryRouter} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {useAuth} from 'react-oidc-context';
import type {AuthContextProps} from 'react-oidc-context';
import {Content} from '../../src/components/Content';

vi.mock('react-oidc-context');
vi.mock('../../src/pages/Forms', () => ({Forms: () => <div>forms-page</div>}));
vi.mock('../../src/pages/FormSubmissions', () => ({FormSubmissions: () => <div>submissions-page</div>}));

const mockSigninRedirect = vi.fn();

function renderContent(path: string) {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={[path]}>
        <Content/>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

function mockAuth(isAuthenticated: boolean) {
  vi.mocked(useAuth).mockReturnValue({
    isAuthenticated,
    isLoading: false,
    signinRedirect: mockSigninRedirect,
    signoutRedirect: vi.fn(),
  } as unknown as AuthContextProps);
}

describe('Content routing', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('redirects unauthenticated user from a protected route to the login page without navigation', async () => {
    const user = userEvent.setup();
    mockAuth(false);
    renderContent('/submissions');

    expect(screen.queryByText('submissions-page')).not.toBeInTheDocument();
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
    expect(screen.getByText('content.pleaseLogIn')).toBeInTheDocument();

    await user.click(screen.getByRole('button', {name: 'navigation.login'}));
    expect(mockSigninRedirect).toHaveBeenCalled();
  });

  it('shows navigation and the requested page for authenticated user', () => {
    mockAuth(true);
    renderContent('/submissions');

    expect(screen.getByRole('navigation')).toBeInTheDocument();
    expect(screen.getByText('submissions-page')).toBeInTheDocument();
  });

  it('redirects authenticated user away from the login page', () => {
    mockAuth(true);
    renderContent('/login');

    expect(screen.getByText('forms-page')).toBeInTheDocument();
    expect(screen.queryByText('content.pleaseLogIn')).not.toBeInTheDocument();
  });
});
