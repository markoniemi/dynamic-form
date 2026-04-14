import type { OidcConfig } from '../types/oidcConfig';

/**
 * Fetch OIDC configuration from the backend /api/config/oauth2-issuer-uri endpoint.
 * Caching is handled by React Query via useQuery with staleTime: Infinity.
 */
export async function loadOidcConfig(): Promise<OidcConfig> {
  const response = await fetch('/api/config/oauth2-issuer-uri');
  if (!response.ok) {
    throw new Error(`Failed to load OIDC config: ${response.statusText}`);
  }

  const issuerUri = await response.text();

  return {
    authority: issuerUri,
    client_id: 'frontend-client',
    redirect_uri: window.location.origin,
    response_type: 'code',
    scope: 'openid profile',
    post_logout_redirect_uri: window.location.origin,
  };
}
