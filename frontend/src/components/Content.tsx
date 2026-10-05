import React from 'react';
import {Navigate, Outlet, Route, Routes} from 'react-router-dom';
import {useAuth} from 'react-oidc-context';
import {useTranslation} from 'react-i18next';
import {Button, Card, Col, Container, Row, Spinner} from 'react-bootstrap';
import {Navigation} from './Navigation';
import {Forms} from '../pages/Forms';
import {FormSubmission} from '../pages/FormSubmission';
import {FormSubmissions} from '../pages/FormSubmissions';
import {SubmissionDetail} from '../pages/SubmissionDetail';
import {EditForm} from '../pages/EditForm.tsx';
import {Login} from '../pages/Login';

const RequireAuth: React.FC = () => {
  const {isAuthenticated} = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace/>;
  }

  return (
    <>
      <Navigation/>
      <Outlet/>
    </>
  );
};

export const Content: React.FC = () => {
  const {isLoading, error, signinRedirect} = useAuth();
  const {t} = useTranslation();

  if (isLoading) {
    return (
      <Container
        className="d-flex justify-content-center align-items-center"
        style={{height: '100vh'}}
      >
        <Spinner animation="border" role="status">
          <span className="visually-hidden">{t('common.loading')}</span>
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
                <Card.Title className="text-danger">{t('content.authError')}</Card.Title>
                <Card.Text>{error.message}</Card.Text>
                <Button variant="primary" onClick={() => signinRedirect()}>
                  {t('content.tryAgain')}
                </Button>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Container>
    );
  }

  return (
    <Routes>
      <Route path="/login" element={<Login/>}/>
      <Route element={<RequireAuth/>}>
        <Route path="/" element={<Forms/>}/>
        <Route path="/forms" element={<Forms/>}/>
        <Route path="/forms/:formKey" element={<FormSubmission/>}/>
        <Route path="/forms/:formKey/submissions/:id/edit" element={<FormSubmission/>}/>
        <Route path="/forms/submissions/:id" element={<SubmissionDetail/>}/>
        <Route path="/create-form" element={<EditForm/>}/>
        <Route path="/submissions" element={<FormSubmissions/>}/>
      </Route>
      <Route path="*" element={<Navigate to="/" replace/>}/>
    </Routes>
  );
};
