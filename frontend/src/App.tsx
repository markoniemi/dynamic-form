import {BrowserRouter as Router} from 'react-router-dom';
import {AuthProvider} from 'react-oidc-context';
import 'bootstrap/dist/css/bootstrap.min.css';
import {useQuery} from '@tanstack/react-query';
import {loadOidcConfig} from './services/configClient';

import {Content} from './components/Content';

function App() {
  const {data: oidcConfig, isLoading, error} = useQuery({
    queryKey: ['oidcConfig'],
    queryFn: loadOidcConfig,
    staleTime: Infinity,
  });

  if (error) {
    return <div className="alert alert-danger m-4">Failed to load OIDC configuration</div>;
  }

  if (isLoading || !oidcConfig) {
    return <div className="spinner-border m-4" role="status"><span className="visually-hidden">Loading...</span></div>;
  }

  return (
    <Router>
      <AuthProvider {...oidcConfig}>
        <Content/>
      </AuthProvider>
    </Router>
  );
}

export default App;
