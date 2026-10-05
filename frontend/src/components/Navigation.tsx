import React from 'react';
import {Button, Container, Nav, Navbar, NavDropdown} from 'react-bootstrap';
import {useAuth} from 'react-oidc-context';
import {Link} from 'react-router-dom';
import {useTranslation} from 'react-i18next';
import {useQueryClient} from '@tanstack/react-query';

export const Navigation: React.FC = () => {
  const {signoutRedirect} = useAuth();
  const {t, i18n} = useTranslation();
  const queryClient = useQueryClient();

  const handleLogout = () => {
    queryClient.clear(); // Clear the cache
    signoutRedirect();
  };

  const changeLanguage = (language: string) => {
    i18n.changeLanguage(language);
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
            <Nav.Link as={Link} to="/forms">
              {t('navigation.forms')}
            </Nav.Link>
            <Nav.Link as={Link} to="/submissions">
              {t('navigation.submissions')}
            </Nav.Link>
            <Nav.Link as={Link} to="/create-form">
              {t('navigation.createForm')}
            </Nav.Link>
          </Nav>
          <Nav>
            <NavDropdown title={t('navigation.language')} id="language-switcher">
              <NavDropdown.Item onClick={() => changeLanguage('en')}>{t('navigation.english')}</NavDropdown.Item>
              {/* Add more languages here */}
            </NavDropdown>
            <div className="d-flex align-items-center ms-2">
              <Button variant="danger" size="sm" onClick={handleLogout}>
                {t('navigation.logout')}
              </Button>
            </div>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};
