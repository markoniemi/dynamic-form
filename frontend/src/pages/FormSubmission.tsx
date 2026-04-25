import React, {useEffect, useState} from 'react';
import {Alert, Button, Card, Container, Form, Spinner} from 'react-bootstrap';
import {useNavigate, useParams} from 'react-router-dom';
import {useForm} from 'react-hook-form';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {useAuth} from 'react-oidc-context';
import {ApiValidationError} from '../services/http';
import {formClient} from '../services/formClient';
import {formDataClient} from '../services/formDataClient';
import {DynamicForm} from '../components/DynamicForm.tsx';
import {useTranslation} from 'react-i18next';

export const FormSubmission: React.FC = () => {
  const {formKey, id} = useParams<{ formKey: string; id?: string }>();
  const navigate = useNavigate();
  const {user, isAuthenticated, signinRedirect} = useAuth();
  const [showSuccess, setShowSuccess] = useState(false);
  const token = user?.access_token;
  const {t} = useTranslation();
  const isEditMode = !!id;
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    setError,
    formState: {errors},
  } = useForm();

  const {
    data: form,
    isLoading: isLoadingForm,
    error: fetchError,
  } = useQuery({
    queryKey: ['form', formKey],
    queryFn: () => formClient.getForm(formKey ?? '', token ?? ''),
    enabled: !!formKey && !!token,
  });

  const {
    data: submission,
    isLoading: isLoadingSubmission,
    error: submissionError,
  } = useQuery({
    queryKey: ['submission', id],
    queryFn: () => formDataClient.getSubmissionById(Number(id), token ?? ''),
    enabled: !!id && !!token,
  });

  useEffect(() => {
    if (submission && submission.data) {
      Object.entries(submission.data).forEach(([key, value]) => {
        setValue(key, value);
      });
    }
  }, [submission, setValue]);

  const mutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => {
      if (!isAuthenticated || !token) {
        return Promise.reject(new Error('You must be logged in to submit a form'));
      }
      if (isEditMode) {
        return formDataClient.updateSubmission(Number(id), data, token);
      }
      return formDataClient.submitForm(formKey ?? '', data, token);
    },
    onSuccess: () => {
      setShowSuccess(true);
      if (!isEditMode) {
        reset();
      }

      // Invalidate queries to ensure fresh data is fetched
      queryClient.invalidateQueries({ queryKey: ['form-submissions'] });
      if (isEditMode) {
        queryClient.invalidateQueries({ queryKey: ['submission', id] });
      }

      setTimeout(() => {
        navigate('/submissions');
      }, 2000);
    },
    onError: (err) => {
      if (err instanceof ApiValidationError) {
        err.validationErrors.forEach(({field, message}) => {
          if (field) {
            setError(field, {message});
          }
        });
      }
    },
  });

  const onSubmit = (data: Record<string, unknown>) => {
    if (!isAuthenticated) {
      signinRedirect();
      return;
    }
    mutation.mutate(data);
  };

  const isLoading = isLoadingForm || (isEditMode && isLoadingSubmission);
  const error = fetchError || (isEditMode && submissionError);

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
        <Button variant="secondary" onClick={() => navigate('/forms')}>
          {t('submissionDetail.back')}
        </Button>
      </Container>
    );
  }

  if (!form) {
    return null;
  }

  return (
    <Container className="mt-5">
      <Card className="shadow-sm">
        <Card.Body>
          <Card.Title as="h2">{isEditMode ? t('form.editTitle', {title: form.title}) : form.title}</Card.Title>
          <Card.Text className="text-muted mb-4">{form.description}</Card.Text>

          {showSuccess && (
            <Alert variant="success" onClose={() => setShowSuccess(false)} dismissible>
              {isEditMode ? t('form.updateSuccess') : t('form.success')}
            </Alert>
          )}

          {mutation.error && (
            <Alert variant="danger">{(mutation.error as Error).message}</Alert>
          )}

          {!isAuthenticated && (
            <Alert variant="warning">
              {t('form.loginRequired')}{' '}
              <Alert.Link onClick={() => signinRedirect()}>{t('navigation.login')}</Alert.Link>
            </Alert>
          )}

          <Form onSubmit={handleSubmit(onSubmit)}>
            <DynamicForm
              fields={form.fields}
              register={register}
              errors={errors}
            />

            <div className="d-flex gap-2">
              <Button
                variant="primary"
                type="submit"
                disabled={mutation.isPending || !isAuthenticated}
              >
                {mutation.isPending ? t('form.submitting') : t('form.submit')}
              </Button>
              <Button variant="secondary" onClick={() => navigate('/submissions')}>
                {t('common.cancel')}
              </Button>
            </div>
          </Form>
        </Card.Body>
      </Card>
    </Container>
  );
};
