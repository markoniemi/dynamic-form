import {vi} from 'vitest';
import {useAuth} from 'react-oidc-context';
import type {AuthContextProps} from 'react-oidc-context';
import type {User} from 'oidc-client-ts';

export const TestUsers = {
  user: {access_token: 'user-token', profile: {sub: 'user'}} as unknown as User,
};

/**
 * Mocks useAuth as a logged-in TestUsers.user; override fields for other states, e.g.
 * mockAuth({user: null, isAuthenticated: false}). The calling test must vi.mock('react-oidc-context').
 */
export function mockAuth(overrides: Partial<AuthContextProps> = {}): AuthContextProps {
  const auth = {
    user: TestUsers.user,
    isAuthenticated: true,
    isLoading: false,
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
    ...overrides,
  } as unknown as AuthContextProps;
  vi.mocked(useAuth).mockReturnValue(auth);
  return auth;
}
