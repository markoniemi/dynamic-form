import React from 'react';
import {Form} from 'react-bootstrap';
import {UseFormRegister} from 'react-hook-form';
import {FormField} from '../types/Form';
import {FieldWrapper} from './FieldWrapper';

interface FieldProps {
  field: FormField;
  register: UseFormRegister<FormValues>;
  errorMessage?: string;
}

export const TextField: React.FC<FieldProps> = ({ field, register, errorMessage }) => (
  <FieldWrapper label={field.label} required={field.required} controlId={field.name}>
    <Form.Control
      type={field.type}
      placeholder={field.placeholder}
      {...register(field.name, { required: field.required ? `${field.label} is required` : false })}
      isInvalid={!!errorMessage}
    />
    <Form.Control.Feedback type="invalid">{errorMessage}</Form.Control.Feedback>
  </FieldWrapper>
);
