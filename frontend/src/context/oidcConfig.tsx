const oidcConfig = {
  authority: 'http://localhost:9000',
  client_id: 'frontend-client',
  redirect_uri: 'http://localhost:8080/',
  response_type: 'code',
  scope: 'openid profile',
  post_logout_redirect_uri: 'http://localhost:8080',
};
export default oidcConfig;
