import React from 'react';
import {Alert, Button, Card, Col, Container, Row, Spinner, Table} from 'react-bootstrap';
import {useAuth} from 'react-oidc-context';
import {useQuery} from '@tanstack/react-query';
import {useNavigate} from 'react-router-dom';
import {formClient} from '../services/formClient';
import {useTranslation} from 'react-i18next';

export const FormSubmissions: React.FC = () => {
  const {user} = useAuth();
  const navigate = useNavigate();
  const token = user?.access_token;
  const {t} = useTranslation();

  const {
    data: submissions = [],
    isLoading,
    error,
  } = useQuery({
    queryKey: ['form-submissions'],
    queryFn: () => formClient.getAllSubmissions(token!),
    enabled: !!token,
  });

  if (isLoading) {
    return (
      <Container className="mt-5 text-center">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">{t('common.loading')}</span>
        </Spinner>
      </Container>
    );
  }

  if (error) {
    return (
      <Container className="mt-5">
        <Alert variant="danger">{(error as Error).message}</Alert>
      </Container>
    );
  }

  return (
    <Container className="mt-5">
      <Row className="mb-4">
        <Col>
          <h2>{t('submissions.title')}</h2>
        </Col>
      </Row>

      {submissions.length === 0 ? (
        <Alert variant="info">{t('submissions.noSubmissions')}</Alert>
      ) : (
        <Card className="shadow-sm">
          <Card.Body>
            <Table striped bordered hover responsive>
              <thead>
              <tr>
                <th>{t('submissions.table.id')}</th>
                <th>{t('submissions.table.formKey')}</th>
                <th>{t('submissions.table.submittedAt')}</th>
                <th>{t('submissions.table.submittedBy')}</th>
                <th>{t('submissions.table.actions')}</th>
              </tr>
              </thead>
              <tbody>
              {submissions.map((submission) => (
                <tr key={submission.id}>
                  <td>{submission.id}</td>
                  <td>{submission.formKey}</td>
                  <td>{t('common.date.long', {date: new Date(submission.submittedAt)})}</td>
                  <td>{submission.submittedBy}</td>
                  <td>
                    <Button
                      variant="outline-primary"
                      size="sm"
                      onClick={() => navigate(`/forms/submissions/${submission.id}`)}
                    >
                      {t('submissions.table.view')}
                    </Button>
                  </td>
                </tr>
              ))}
              </tbody>
            </Table>
          </Card.Body>
        </Card>
      )}
    </Container>
  );
};
