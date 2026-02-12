import React, { useEffect } from 'react';
import { Alert, Button, Container, Form, Spinner } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useAuth } from 'react-oidc-context';
import { itemClient } from '../services/itemClient';

import { ItemDto } from '../types/ItemDto';

const itemSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().optional(),
});

type ItemFormData = z.infer<typeof itemSchema>;

export const ItemForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const isEditMode = !!id;
  const token = user?.access_token;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ItemFormData>({
    resolver: zodResolver(itemSchema),
  });

  const { data, isLoading, error } = useQuery({
    queryKey: ['item', id],
    queryFn: () => itemClient.getItem(Number(id), token!),
    enabled: isEditMode && !!token,
  });

  const mutation = useMutation({
    mutationFn: (data: ItemFormData) => {
      if (isEditMode) {
        return itemClient.updateItem(Number(id), data, token!);
      }
      return itemClient.createItem(
        data as Omit<ItemDto, 'id' | 'createdAt' | 'updatedAt'>,
        token!
      );
    },
    onSuccess: () => navigate('/'),
  });

  useEffect(() => {
    if (data) {
      reset(data);
    }
  }, [data, reset]);

  const onSubmit = (data: ItemFormData) => mutation.mutate(data);

  if (isLoading) {
    return (
      <Container className="mt-5 text-center">
        <Spinner animation="border" role="status" />
      </Container>
    );
  }

  return (
    <Container className="mt-5">
      <h2>{isEditMode ? 'Edit Item' : 'Add New Item'}</h2>
      {error && <Alert variant="danger">{(error as Error).message}</Alert>}
      {mutation.error && <Alert variant="danger">{(mutation.error as Error).message}</Alert>}
      <Form onSubmit={handleSubmit(onSubmit)}>
        <Form.Group className="mb-3" controlId="formName">
          <Form.Label>Name</Form.Label>
          <Form.Control
            type="text"
            placeholder="Enter item name"
            {...register('name')}
            isInvalid={!!errors.name}
          />
          <Form.Control.Feedback type="invalid">{errors.name?.message}</Form.Control.Feedback>
        </Form.Group>

        <Form.Group className="mb-3" controlId="formDescription">
          <Form.Label>Description</Form.Label>
          <Form.Control
            as="textarea"
            rows={3}
            placeholder="Enter item description"
            {...register('description')}
          />
        </Form.Group>

        <Button variant="primary" type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Saving...' : 'Save Item'}
        </Button>
        <Button variant="secondary" className="ms-2" onClick={() => navigate('/')}>
          Cancel
        </Button>
      </Form>
    </Container>
  );
};
