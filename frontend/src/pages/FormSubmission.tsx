import React, { useState } from 'react';
import { Alert, Button, Card, Container, Form, Spinner } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useAuth } from 'react-oidc-context';
import { formClient } from '../services/formClient';
import { DynamicForm } from '../components/DynamicForm.tsx';

export const FormSubmission: React.FC = () => {
  const { formKey } = useParams<{ formKey: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated, signinRedirect } = useAuth();
  const [showSuccess, setShowSuccess] = useState(false);
  const token = user?.access_token;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm();

  const {
    data: formDefinition,
    isLoading,
    error: fetchError,
  } = useQuery({
    queryKey: ['form', formKey],
    queryFn: () => formClient.getFormDefinition(formKey!),
    enabled: !!formKey,
  });

  const mutation = useMutation({
    mutationFn: (data: Record<string, any>) => {
      if (!isAuthenticated || !token) {
        return Promise.reject(new Error('You must be logged in to submit a form'));
      }
      return formClient.submitForm(formKey!, data, token);
    },
    onSuccess: () => {
      setShowSuccess(true);
      reset();
      setTimeout(() => {
        navigate('/forms');
      }, 3000);
    },
  });

  const onSubmit = (data: Record<string, any>) => {
    if (!isAuthenticated) {
      signinRedirect();
      return;
    }
    mutation.mutate(data);
  };

  if (isLoading) {
    return (
      <Container className="mt-5 text-center">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </Container>
    );
  }

  if (fetchError) {
    return (
      <Container className="mt-5">
        <Alert variant="danger">{(fetchError as Error).message}</Alert>
        <Button variant="secondary" onClick={() => navigate('/forms')}>
          Back to Forms
        </Button>
      </Container>
    );
  }

  if (!formDefinition) {
    return null;
  }

  return (
    <Container className="mt-5">
      <Card className="shadow-sm">
        <Card.Body>
          <Card.Title as="h2">{formDefinition.title}</Card.Title>
          <Card.Text className="text-muted mb-4">{formDefinition.description}</Card.Text>

          {showSuccess && (
            <Alert variant="success" onClose={() => setShowSuccess(false)} dismissible>
              Form submitted successfully! Redirecting to forms list...
            </Alert>
          )}

          {mutation.error && (
            <Alert variant="danger">{(mutation.error as Error).message}</Alert>
          )}

          {!isAuthenticated && (
            <Alert variant="warning">
              You must be logged in to submit this form.{' '}
              <Alert.Link onClick={() => signinRedirect()}>Click here to log in</Alert.Link>
            </Alert>
          )}

          <Form onSubmit={handleSubmit(onSubmit)}>
            <DynamicForm
              fields={formDefinition.fields}
              register={register}
              errors={errors}
            />

            <div className="d-flex gap-2">
              <Button
                variant="primary"
                type="submit"
                disabled={mutation.isPending || !isAuthenticated}
              >
                {mutation.isPending ? 'Submitting...' : 'Submit'}
              </Button>
              <Button variant="secondary" onClick={() => navigate('/submissions')}>
                Cancel
              </Button>
            </div>
          </Form>
        </Card.Body>
      </Card>
    </Container>
  );
};
