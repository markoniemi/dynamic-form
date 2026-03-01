import React from 'react';
import {Alert, Button, Card, Col, Container, Row, Spinner} from 'react-bootstrap';
import {useNavigate, useParams} from 'react-router-dom';
import {useQuery} from '@tanstack/react-query';
import {useAuth} from 'react-oidc-context';
import {formClient} from '../services/formClient';
import {ReadOnlyDynamicForm} from '../components/ReadOnlyDynamicForm';
import {useTranslation} from 'react-i18next';

export const SubmissionDetail: React.FC = () => {
  const {id} = useParams<{ id: string }>();
  const navigate = useNavigate();
  const {user} = useAuth();
  const token = user?.access_token;
  const {t} = useTranslation();

  const {
    data: submission,
    isLoading: isLoadingSubmission,
    error: submissionError,
  } = useQuery({
    queryKey: ['submission', id],
    queryFn: () => formClient.getSubmissionById(Number(id), token!),
    enabled: !!token && !!id,
  });

  const {
    data: form,
    isLoading: isLoadingForm,
    error: formError,
  } = useQuery({
    queryKey: ['form', submission?.formKey],
    queryFn: () => formClient.getForm(submission!.formKey, token!),
    enabled: !!submission?.formKey,
  });

  const isLoading = isLoadingSubmission || isLoadingForm;
  const error = submissionError || formError;

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
        <Button variant="secondary" onClick={() => navigate('/submissions')}>
          {t('submissionDetail.back')}
        </Button>
      </Container>
    );
  }

  if (!submission || !form) {
    return (
      <Container className="mt-5">
        <Alert variant="warning">{t('submissionDetail.notFound')}</Alert>
        <Button variant="secondary" onClick={() => navigate('/submissions')}>
          {t('submissionDetail.back')}
        </Button>
      </Container>
    );
  }

  return (
    <Container className="mt-5">
      <Row className="mb-4">
        <Col>
          <Button variant="outline-secondary" onClick={() => navigate('/submissions')}>
            ← {t('submissionDetail.back')}
          </Button>
        </Col>
      </Row>

      <Card className="shadow-sm">
        <Card.Body>
          <Card.Title as="h2">{form.title}</Card.Title>
          <Card.Text className="text-muted mb-4">{form.description}</Card.Text>

          <Alert variant="info" className="mb-4">
            <strong>{t('submissionDetail.id')}:</strong> {submission.id}
            <br/>
            <strong>{t('submissionDetail.formKey')}:</strong> {submission.formKey}
            <br/>
            <strong>{t('submissionDetail.submittedAt')}:</strong> {t('common.date.long', {date: new Date(submission.submittedAt)})}
            <br/>
            <strong>{t('submissionDetail.submittedBy')}:</strong> {submission.submittedBy}
          </Alert>

          <ReadOnlyDynamicForm fields={form.fields} data={submission.data}/>
        </Card.Body>
      </Card>
    </Container>
  );
};
