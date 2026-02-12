import React from 'react';
import { Alert, Button, Card, Col, Container, Row, Spinner } from 'react-bootstrap';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { formClient } from '../services/formClient';
import { FileText } from 'lucide-react';

export const Forms: React.FC = () => {
  const navigate = useNavigate();

  const {
    data: formKeys = [],
    isLoading,
    error,
  } = useQuery({
    queryKey: ['forms'],
    queryFn: () => formClient.getAvailableForms(),
  });

  const formatFormKey = (key: string) => {
    return key
      .split('-')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  };

  return (
    <Container className="mt-5">
      <Row className="mb-4">
        <Col>
          <h2>Available Forms</h2>
          <p className="text-muted">Select a form to view or submit</p>
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
      ) : formKeys.length === 0 ? (
        <Alert variant="info">No forms available</Alert>
      ) : (
        <Row>
          {formKeys.map((formKey) => (
            <Col md={4} key={formKey} className="mb-4">
              <Card className="h-100 shadow-sm">
                <Card.Body className="d-flex flex-column">
                  <div className="mb-3 text-center">
                    <FileText size={48} className="text-primary" />
                  </div>
                  <Card.Title className="text-center">{formatFormKey(formKey)}</Card.Title>
                  <div className="mt-auto">
                    <Button
                      variant="primary"
                      className="w-100"
                      onClick={() => navigate(`/forms/${formKey}`)}
                    >
                      Open Form
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
