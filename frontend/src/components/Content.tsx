import React from 'react';
import {Navigate, Route, Routes} from 'react-router-dom';
import {useAuth} from 'react-oidc-context';
import {Button, Card, Col, Container, Row, Spinner} from 'react-bootstrap';
import {Navigation} from './Navigation';
import {Forms} from '../pages/Forms';
import {FormSubmission} from '../pages/FormSubmission';
import {FormSubmissions} from '../pages/FormSubmissions';

export const Content: React.FC = () => {
  const { isAuthenticated, isLoading, error, signinRedirect } = useAuth();

  if (isLoading) {
    return (
      <Container
        className="d-flex justify-content-center align-items-center"
        style={{ height: '100vh' }}
      >
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </Container>
    );
  }

  if (error) {
    return (
      <Container className="mt-5">
        <Row>
          <Col md={6} className="mx-auto text-center">
            <Card border="danger">
              <Card.Body>
                <Card.Title className="text-danger">Authentication Error</Card.Title>
                <Card.Text>{error.message}</Card.Text>
                <Button variant="primary" onClick={() => signinRedirect()}>
                  Try Again
                </Button>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Container>
    );
  }

  return (
    <>
      <Navigation />
      <Routes>
        {/* Public routes - Forms can be viewed by anyone */}

        {/* Protected routes - Require authentication */}
        {isAuthenticated ? (
          <>
            <Route path="/forms" element={<Forms />} />
            <Route path="/forms/:formKey" element={<FormSubmission />} />
            <Route path="/" element={<Forms />} />
            <Route path="/submissions" element={<FormSubmissions />} />
          </>
        ) : (
          <Route
            path="/"
            element={
              <Container className="mt-5">
                <Row>
                  <Col md={6} className="mx-auto text-center">
                    <Card>
                      <Card.Body>
                        <Card.Title>Welcome</Card.Title>
                        <Card.Text>Please log in.</Card.Text>
                        <Button variant="primary" onClick={() => signinRedirect()}>
                          Login with OAuth2
                        </Button>
                      </Card.Body>
                    </Card>
                  </Col>
                </Row>
              </Container>
            }
          />
        )}

        {/* Redirect unknown routes */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
};
