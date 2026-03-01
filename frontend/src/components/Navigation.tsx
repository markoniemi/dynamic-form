import React from 'react';
import {Button, Container, Nav, Navbar, NavDropdown} from 'react-bootstrap';
import {useAuth} from 'react-oidc-context';
import {Link} from 'react-router-dom';
import {useTranslation} from 'react-i18next';

export const Navigation: React.FC = () => {
  const {isAuthenticated, signinRedirect, signoutRedirect} = useAuth();
  const {t, i18n} = useTranslation();

  const changeLanguage = (lng: string) => {
    i18n.changeLanguage(lng);
  };

  return (
    <Navbar bg="dark" data-bs-theme="dark" sticky="top" expand="lg">
      <Container>
        <Navbar.Brand as={Link} to="/">
          {t('navigation.brand')}
        </Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav"/>
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="me-auto">
            {isAuthenticated && (
              <>
                <Nav.Link as={Link} to="/submissions">
                  {t('navigation.submissions')}
                </Nav.Link>
                <Nav.Link as={Link} to="/create-form">
                  Create Form
                </Nav.Link>
              </>
            )}
            <Nav.Link as={Link} to="/forms">
              {t('navigation.forms')}
            </Nav.Link>
          </Nav>
          <Nav>
            <NavDropdown title="Language" id="language-switcher">
              <NavDropdown.Item onClick={() => changeLanguage('en')}>English</NavDropdown.Item>
              {/* Add more languages here */}
            </NavDropdown>
            <div className="d-flex align-items-center ms-2">
            {isAuthenticated ? (
              <Button variant="danger" size="sm" onClick={() => signoutRedirect()}>
                {t('navigation.logout')}
              </Button>
            ) : (
              <Button variant="primary" size="sm" onClick={() => signinRedirect()}>
                {t('navigation.login')}
              </Button>
            )}
            </div>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};
