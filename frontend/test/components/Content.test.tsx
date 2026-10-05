import {screen} from '@testing-library/react';
import {userEvent} from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Content} from '../../src/components/Content';
import {renderWithProviders} from '../renderWithProviders';
import {mockAuth} from '../testdata/TestUsers';

vi.mock('react-oidc-context');
vi.mock('../../src/pages/Forms', () => ({Forms: () => <div>forms-page</div>}));
vi.mock('../../src/pages/FormSubmissions', () => ({FormSubmissions: () => <div>submissions-page</div>}));

describe('Content routing', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('redirects unauthenticated user from a protected route to the login page without navigation', async () => {
    const user = userEvent.setup();
    const auth = mockAuth({user: null, isAuthenticated: false});
    renderWithProviders(<Content/>, {route: '/submissions'});

    expect(screen.queryByText('submissions-page')).not.toBeInTheDocument();
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
    expect(screen.getByText('content.pleaseLogIn')).toBeInTheDocument();

    await user.click(screen.getByRole('button', {name: 'navigation.login'}));
    expect(auth.signinRedirect).toHaveBeenCalled();
  });

  it('shows navigation and the requested page for authenticated user', () => {
    mockAuth();
    renderWithProviders(<Content/>, {route: '/submissions'});

    expect(screen.getByRole('navigation')).toBeInTheDocument();
    expect(screen.getByText('submissions-page')).toBeInTheDocument();
  });

  it('redirects authenticated user away from the login page', () => {
    mockAuth();
    renderWithProviders(<Content/>, {route: '/login'});

    expect(screen.getByText('forms-page')).toBeInTheDocument();
    expect(screen.queryByText('content.pleaseLogIn')).not.toBeInTheDocument();
  });
});
