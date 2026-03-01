import React from 'react';
import {Alert, Button, Col, Container, Row, Spinner, Table} from 'react-bootstrap';
import {useQuery} from '@tanstack/react-query';
import {useNavigate} from 'react-router-dom';
import {formClient} from '../services/formClient';
import {FileText} from 'lucide-react';
import {useTranslation} from 'react-i18next';
import {useAuth} from "react-oidc-context";

export const Forms: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const token = user?.access_token;
  const { t } = useTranslation();

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
          <h2>{t('forms.title')}</h2>
          <p className="text-muted">{t('forms.subtitle')}</p>
        </Col>
      </Row>

      {error && (
        <Alert variant="danger">{(error as Error).message || t('common.error')}</Alert>
      )}

      {isLoading ? (
        <div className="text-center">
          <Spinner animation="border" role="status">
            <span className="visually-hidden">{t('common.loading')}</span>
          </Spinner>
        </div>
      ) : forms.length === 0 ? (
        <Alert variant="info">{t('forms.noForms')}</Alert>
      ) : (
        <Table striped bordered hover responsive className="shadow-sm">
          <thead className="table-light">
            <tr>
              <th scope="col">
                <FileText size={20} className="me-2" />
                {t('forms.table.name')}
              </th>
              <th scope="col" className="text-center">
                {t('forms.table.actions')}
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
                    {t('forms.table.open')}
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
