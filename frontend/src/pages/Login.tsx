import React from 'react';
import {Navigate} from 'react-router-dom';
import {useAuth} from 'react-oidc-context';
import {useTranslation} from 'react-i18next';
import {Button, Card, Col, Container, Row} from 'react-bootstrap';

export const Login: React.FC = () => {
  const {isAuthenticated, signinRedirect} = useAuth();
  const {t} = useTranslation();

  if (isAuthenticated) {
    return <Navigate to="/" replace/>;
  }

  return (
    <Container className="mt-5">
      <Row>
        <Col md={6} className="mx-auto text-center">
          <Card>
            <Card.Body>
              <Card.Title>{t('content.welcome')}</Card.Title>
              <Card.Text>{t('content.pleaseLogIn')}</Card.Text>
              <Button variant="primary" onClick={() => signinRedirect()}>
                {t('navigation.login')}
              </Button>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};
