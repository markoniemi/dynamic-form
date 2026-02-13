import React from 'react';
import { Form } from 'react-bootstrap';
import { UseFormRegister, FieldErrors } from 'react-hook-form';
import { FormField } from '../types/Form';

interface DynamicFormProps {
  fields: FormField[];
  register: UseFormRegister<any>;
  errors: FieldErrors;
}

export const DynamicForm: React.FC<DynamicFormProps> = ({
  fields,
  register,
  errors,
}) => {
  const renderField = (field: FormField) => {
    const errorMessage = errors[field.name]?.message as string | undefined;

    switch (field.type) {
      case 'text':
      case 'email':
      case 'tel':
      case 'number':
      case 'date':
        return (
          <Form.Group className="mb-3" key={field.name} controlId={field.name}>
            <Form.Label>
              {field.label}
              {field.required && <span className="text-danger"> *</span>}
            </Form.Label>
            <Form.Control
              type={field.type}
              placeholder={field.placeholder}
              {...register(field.name, { required: field.required ? `${field.label} is required` : false })}
              isInvalid={!!errorMessage}
            />
            <Form.Control.Feedback type="invalid">{errorMessage}</Form.Control.Feedback>
          </Form.Group>
        );

      case 'textarea':
        return (
          <Form.Group className="mb-3" key={field.name} controlId={field.name}>
            <Form.Label>
              {field.label}
              {field.required && <span className="text-danger"> *</span>}
            </Form.Label>
            <Form.Control
              as="textarea"
              rows={4}
              placeholder={field.placeholder}
              {...register(field.name, { required: field.required ? `${field.label} is required` : false })}
              isInvalid={!!errorMessage}
            />
            <Form.Control.Feedback type="invalid">{errorMessage}</Form.Control.Feedback>
          </Form.Group>
        );

      case 'select':
        return (
          <Form.Group className="mb-3" key={field.name} controlId={field.name}>
            <Form.Label>
              {field.label}
              {field.required && <span className="text-danger"> *</span>}
            </Form.Label>
            <Form.Select
              {...register(field.name, { required: field.required ? `${field.label} is required` : false })}
              isInvalid={!!errorMessage}
            >
              <option value="">Select an option...</option>
              {field.options?.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Form.Select>
            <Form.Control.Feedback type="invalid">{errorMessage}</Form.Control.Feedback>
          </Form.Group>
        );

      case 'radio':
        return (
          <Form.Group className="mb-3" key={field.name}>
            <Form.Label>
              {field.label}
              {field.required && <span className="text-danger"> *</span>}
            </Form.Label>
            {field.options?.map((option) => (
              <Form.Check
                key={option.value}
                type="radio"
                id={`${field.name}-${option.value}`}
                label={option.label}
                value={option.value}
                {...register(field.name, { required: field.required ? `${field.label} is required` : false })}
                isInvalid={!!errorMessage}
              />
            ))}
            {errorMessage && <div className="invalid-feedback d-block">{errorMessage}</div>}
          </Form.Group>
        );

      case 'checkbox':
        return (
          <Form.Group className="mb-3" key={field.name}>
            <Form.Label>
              {field.label}
              {field.required && <span className="text-danger"> *</span>}
            </Form.Label>
            {field.options?.map((option) => (
              <Form.Check
                key={option.value}
                type="checkbox"
                id={`${field.name}-${option.value}`}
                label={option.label}
                value={option.value}
                {...register(field.name)}
              />
            ))}
            {errorMessage && <div className="invalid-feedback d-block">{errorMessage}</div>}
          </Form.Group>
        );

      default:
        return null;
    }
  };

  return <>{fields.map(renderField)}</>;
};
