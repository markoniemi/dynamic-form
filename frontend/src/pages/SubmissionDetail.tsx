import React from 'react';
import { Alert, Button, Card, Col, Container, Row, Spinner } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from 'react-oidc-context';
import { formClient } from '../services/formClient';
import { ReadOnlyDynamicForm } from '../components/ReadOnlyDynamicForm';

export const SubmissionDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const token = user?.access_token;

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
    data: formDefinition,
    isLoading: isLoadingForm,
    error: formError,
  } = useQuery({
    queryKey: ['form', submission?.formKey],
    queryFn: () => formClient.getFormDefinition(submission!.formKey),
    enabled: !!submission?.formKey,
  });

  const isLoading = isLoadingSubmission || isLoadingForm;
  const error = submissionError || formError;

  if (isLoading) {
    return (
      <Container className="mt-5 text-center">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </Container>
    );
  }

  if (error) {
    return (
      <Container className="mt-5">
        <Alert variant="danger">{(error as Error).message}</Alert>
        <Button variant="secondary" onClick={() => navigate('/submissions')}>
          Back to Submissions
        </Button>
      </Container>
    );
  }

  if (!submission || !formDefinition) {
    return (
      <Container className="mt-5">
        <Alert variant="warning">Submission not found.</Alert>
        <Button variant="secondary" onClick={() => navigate('/submissions')}>
          Back to Submissions
        </Button>
      </Container>
    );
  }

  return (
    <Container className="mt-5">
      <Row className="mb-4">
        <Col>
          <Button variant="outline-secondary" onClick={() => navigate('/submissions')}>
            ← Back to Submissions
          </Button>
        </Col>
      </Row>

      <Card className="shadow-sm">
        <Card.Body>
          <Card.Title as="h2">{formDefinition.title}</Card.Title>
          <Card.Text className="text-muted mb-4">{formDefinition.description}</Card.Text>

          <Alert variant="info" className="mb-4">
            <strong>Submission ID:</strong> {submission.id}
            <br />
            <strong>Form Key:</strong> {submission.formKey}
            <br />
            <strong>Submitted At:</strong> {new Date(submission.submittedAt).toLocaleString()}
          </Alert>

          <ReadOnlyDynamicForm fields={formDefinition.fields} data={submission.data} />
        </Card.Body>
      </Card>
    </Container>
  );
};

