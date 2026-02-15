import React, {useState} from 'react';
import {Alert, Button, Card, Col, Container, Form, Row, Spinner,} from 'react-bootstrap';
import {useNavigate} from 'react-router-dom';
import {useMutation} from '@tanstack/react-query';
import {useAuth} from 'react-oidc-context';
import {formClient} from '../services/formClient';
import {CreateForm, FormField} from '../types/Form';
import {FieldEditor} from '../components/FieldEditor';

const FIELD_TYPES = [
  { value: 'text', label: 'Text' },
  { value: 'email', label: 'Email' },
  { value: 'tel', label: 'Phone' },
  { value: 'number', label: 'Number' },
  { value: 'date', label: 'Date' },
  { value: 'textarea', label: 'Text Area' },
  { value: 'select', label: 'Dropdown' },
  { value: 'radio', label: 'Radio Buttons' },
  { value: 'checkbox', label: 'Checkbox' },
] as const;

const createEmptyField = (): FormField => ({
  name: '',
  label: '',
  type: 'text',
  required: false,
  placeholder: '',
  options: [],
});

export const EditForm: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const token = user?.access_token;

  const [formKey, setFormKey] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [fields, setFields] = useState<FormField[]>([createEmptyField()]);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (form: CreateForm) =>
      formClient.saveForm(form, token!),
    onSuccess: () => {
      navigate('/forms');
    },
    onError: (err: Error) => {
      setError(err.message);
    },
  });

  const handleAddField = () => {
    setFields([...fields, createEmptyField()]);
  };

  const handleRemoveField = (index: number) => {
    if (fields.length > 1) {
      setFields(fields.filter((_, i) => i !== index));
    }
  };

  const handleFieldChange = (index: number, updatedField: FormField) => {
    const newFields = [...fields];
    newFields[index] = updatedField;
    setFields(newFields);
  };

  const handleMoveField = (index: number, direction: 'up' | 'down') => {
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= fields.length) return;

    const newFields = [...fields];
    [newFields[index], newFields[newIndex]] = [newFields[newIndex], newFields[index]];
    setFields(newFields);
  };

  const validateForm = (): boolean => {
    if (!formKey.trim()) {
      setError('Form key is required');
      return false;
    }
    if (!/^[a-z0-9-]+$/.test(formKey)) {
      setError('Form key must contain only lowercase letters, numbers, and hyphens');
      return false;
    }
    if (!title.trim()) {
      setError('Title is required');
      return false;
    }
    if (fields.length === 0) {
      setError('At least one field is required');
      return false;
    }

    const fieldNames = new Set<string>();
    for (const field of fields) {
      if (!field.name.trim()) {
        setError('All fields must have a name');
        return false;
      }
      if (!/^[a-zA-Z][a-zA-Z0-9]*$/.test(field.name)) {
        setError(`Field name "${field.name}" must start with a letter and contain only alphanumeric characters`);
        return false;
      }
      if (fieldNames.has(field.name)) {
        setError(`Duplicate field name: ${field.name}`);
        return false;
      }
      fieldNames.add(field.name);

      if (!field.label.trim()) {
        setError('All fields must have a label');
        return false;
      }

      if ((field.type === 'select' || field.type === 'radio') && (!field.options || field.options.length === 0)) {
        setError(`Field "${field.label}" requires at least one option`);
        return false;
      }
    }

    setError(null);
    return true;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    const form: CreateForm = {
      formKey,
      title,
      description,
      fields,
    };

    mutation.mutate(form);
  };

  return (
    <Container className="mt-5">
      <Row className="mb-4">
        <Col>
          <Button variant="outline-secondary" onClick={() => navigate('/forms')}>
            ← Back to Forms
          </Button>
        </Col>
      </Row>

      <Card className="shadow-sm">
        <Card.Body>
          <Card.Title as="h2">Create New Form</Card.Title>

          {error && <Alert variant="danger" className="mt-3">{error}</Alert>}

          <Form onSubmit={handleSubmit} className="mt-4">
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3" controlId="formKey">
                  <Form.Label>Form Key <span className="text-danger">*</span></Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="e.g., contact-form"
                    value={formKey}
                    onChange={(e) => setFormKey(e.target.value.toLowerCase())}
                  />
                  <Form.Text className="text-muted">
                    Unique identifier (lowercase letters, numbers, hyphens only)
                  </Form.Text>
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3" controlId="title">
                  <Form.Label>Title <span className="text-danger">*</span></Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="e.g., Contact Form"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                  />
                </Form.Group>
              </Col>
            </Row>

            <Form.Group className="mb-4" controlId="description">
              <Form.Label>Description</Form.Label>
              <Form.Control
                as="textarea"
                rows={2}
                placeholder="Brief description of the form"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </Form.Group>

            <hr />
            <h4 className="mb-3">Fields</h4>

            {fields.map((field, index) => (
              <FieldEditor
                key={index}
                field={field}
                index={index}
                totalFields={fields.length}
                fieldTypes={FIELD_TYPES}
                onChange={(updatedField) => handleFieldChange(index, updatedField)}
                onRemove={() => handleRemoveField(index)}
                onMove={(direction) => handleMoveField(index, direction)}
              />
            ))}

            <Button variant="outline-primary" onClick={handleAddField} className="mb-4">
              + Add Field
            </Button>

            <hr />

            <div className="d-flex gap-2">
              <Button variant="primary" type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? (
                  <>
                    <Spinner animation="border" size="sm" className="me-2" />
                    Creating...
                  </>
                ) : (
                  'Create Form'
                )}
              </Button>
              <Button variant="secondary" onClick={() => navigate('/forms')}>
                Cancel
              </Button>
            </div>
          </Form>
        </Card.Body>
      </Card>
    </Container>
  );
};

