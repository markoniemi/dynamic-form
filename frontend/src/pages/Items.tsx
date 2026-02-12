import React from 'react';
import { Alert, Button, Card, Col, Container, Row, Spinner } from 'react-bootstrap';
import { useAuth } from 'react-oidc-context';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { itemClient } from '../services/itemClient';

export const Items: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const token = user?.access_token;

  const {
    data: items = [],
    isLoading,
    error: fetchError,
  } = useQuery({
    queryKey: ['items'],
    queryFn: () => itemClient.getItems(token!),
    enabled: !!token,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => itemClient.deleteItem(id, token!),
    onSuccess: () => {
      // Invalidate the query to refetch the list after a successful deletion
      queryClient.invalidateQueries({ queryKey: ['items'] });
    },
  });

  const handleDelete = (id: number) => {
    if (window.confirm('Are you sure you want to delete this item?')) {
      deleteMutation.mutate(id);
    }
  };

  const error = fetchError || deleteMutation.error;

  return (
    <Container className="mt-5">
      <Row className="mb-4">
        <Col>
          <h2>Items</h2>
        </Col>
        <Col className="text-end">
          <Button variant="success" onClick={() => navigate('/item/new')}>
            Add Item
          </Button>
        </Col>
      </Row>

      {error && (
        <Alert variant="danger">{(error as Error).message || 'An error occurred'}</Alert>
      )}

      {isLoading ? (
        <div className="text-center">
          <Spinner animation="border" role="status">
            <span className="visually-hidden">Loading...</span>
          </Spinner>
        </div>
      ) : items.length === 0 ? (
        <Alert variant="info">No items found</Alert>
      ) : (
        <Row>
          {items.map((item) => (
            <Col md={4} key={item.id} className="mb-4">
              <Card>
                <Card.Body>
                  <Card.Title>{item.name}</Card.Title>
                  <Card.Text>{item.description}</Card.Text>
                  <small className="text-muted">
                    Created: {new Date(item.createdAt).toLocaleDateString()}
                  </small>
                  <div className="mt-3">
                    <Button
                      variant="primary"
                      size="sm"
                      className="me-2"
                      onClick={() => navigate(`/item/${item.id}`)}
                    >
                      Edit
                    </Button>
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={() => handleDelete(item.id)}
                      disabled={deleteMutation.isPending}
                    >
                      Delete
                    </Button>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </Container>
  );
};
