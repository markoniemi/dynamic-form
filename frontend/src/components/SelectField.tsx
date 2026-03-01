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

export const SelectField: React.FC<FieldProps> = ({field, register, errorMessage}) => (
  <FieldWrapper label={field.label} required={field.required} controlId={field.name}>
    <Form.Select
      {...register(field.name, {required: field.required ? `${field.label} is required` : false})}
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
  </FieldWrapper>
);
