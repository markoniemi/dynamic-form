import React from 'react';
import { Alert, Button, Card, Col, Container, Row, Spinner, Table } from 'react-bootstrap';
import { useAuth } from 'react-oidc-context';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { formClient } from '../services/formClient';

export const FormSubmissions: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const token = user?.access_token;

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
          <span className="visually-hidden">Loading...</span>
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
          <h2>Form Submissions</h2>
        </Col>
      </Row>

      {submissions.length === 0 ? (
        <Alert variant="info">No submissions found.</Alert>
      ) : (
        <Card className="shadow-sm">
          <Card.Body>
            <Table striped bordered hover responsive>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Form Key</th>
                  <th>Submitted At</th>
                  <th>Data</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {submissions.map((submission) => (
                  <tr key={submission.id}>
                    <td>{submission.id}</td>
                    <td>{submission.formKey}</td>
                    <td>{new Date(submission.submittedAt).toLocaleString()}</td>
                    <td>
                      <pre style={{ margin: 0, maxHeight: '100px', overflowY: 'auto' }}>
                        {JSON.stringify(submission.data, null, 2)}
                      </pre>
                    </td>
                    <td>
                      <Button
                        variant="outline-primary"
                        size="sm"
                        onClick={() => navigate(`/forms/submissions/${submission.id}`)}
                      >
                        View Details
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
