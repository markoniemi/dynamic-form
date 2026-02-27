import React from 'react';
import { Alert, Button, Container, Row, Col, Spinner, Table } from 'react-bootstrap';
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
        <Table striped bordered hover responsive className="shadow-sm">
          <thead className="table-light">
            <tr>
              <th scope="col">
                <FileText size={20} className="me-2" />
                Form Name
              </th>
              <th scope="col" className="text-center">
                Actions
              </th>
            </tr>
          </thead>
          <tbody>
            {formKeys.map((formKey) => (
              <tr key={formKey}>
                <td className="align-middle">{formatFormKey(formKey)}</td>
                <td className="text-center">
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => navigate(`/forms/${formKey}`)}
                  >
                    Open Form
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </Container>
  );
};
