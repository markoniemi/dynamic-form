/**
 * OIDC (OpenID Connect) configuration interface for react-oidc-context
 */
export interface OidcConfig {
  authority: string;
  client_id: string;
  redirect_uri: string;
  response_type: string;
  scope: string;
  post_logout_redirect_uri: string;
}
