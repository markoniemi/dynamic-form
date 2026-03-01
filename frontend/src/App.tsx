import {BrowserRouter as Router} from 'react-router-dom';
import {AuthProvider} from 'react-oidc-context';
import 'bootstrap/dist/css/bootstrap.min.css';
import oidcConfig from './context/oidcConfig.tsx';

import {Content} from './components/Content';

function App() {
  return (
    <Router>
      <AuthProvider {...oidcConfig}>
        <Content/>
      </AuthProvider>
    </Router>
  );
}

export default App;
