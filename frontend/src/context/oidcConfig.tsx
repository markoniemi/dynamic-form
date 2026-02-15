const oidcConfig = {
  authority: 'http://localhost:9000',
  client_id: 'frontend-client',
  redirect_uri: window.location.origin,
  response_type: 'code',
  scope: 'openid profile',
  post_logout_redirect_uri: window.location.origin,
};
export default oidcConfig;
