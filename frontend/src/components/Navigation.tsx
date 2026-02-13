import React from 'react';
import { Button, Container, Nav, Navbar } from 'react-bootstrap';
import { useAuth } from 'react-oidc-context';
import { Link } from 'react-router-dom';

export const Navigation: React.FC = () => {
  const { isAuthenticated, signinRedirect, signoutRedirect } = useAuth();

  return (
    <Navbar bg="dark" data-bs-theme="dark" sticky="top" expand="lg">
      <Container>
        <Navbar.Brand as={Link} to="/">
          dynamic-form
        </Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav" />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="me-auto">
            {isAuthenticated && (
              <>
                <Nav.Link as={Link} to="/submissions">
                  Submissions
                </Nav.Link>
              </>
            )}
            <Nav.Link as={Link} to="/forms">
              Forms
            </Nav.Link>
          </Nav>
          <Nav>
            {isAuthenticated ? (
              <Button variant="danger" size="sm" onClick={() => signoutRedirect()}>
                Logout
              </Button>
            ) : (
              <Button variant="primary" size="sm" onClick={() => signinRedirect()}>
                Login
              </Button>
            )}
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};
