import React from 'react';
import { Alert, Button, Container, Row, Col, Spinner, Table } from 'react-bootstrap';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { formClient } from '../services/formClient';
import { FileText } from 'lucide-react';
import {useAuth} from "react-oidc-context";

export const Forms: React.FC = () => {
  const navigate = useNavigate();
    const { user } = useAuth();
    const token = user?.access_token;

  const {
    data: forms = [],
    isLoading,
    error,
  } = useQuery({
    queryKey: ['forms'],
    queryFn: () => formClient.getAvailableForms(token!),
  });

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
      ) : forms.length === 0 ? (
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
            {forms.map((form) => (
              <tr key={form.formKey}>
                <td className="align-middle">{form.title}</td>
                <td className="text-center">
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => navigate(`/forms/${form.formKey}`)}
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
