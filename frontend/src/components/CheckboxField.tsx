import React from 'react';
import { Form } from 'react-bootstrap';
import { UseFormRegister } from 'react-hook-form';
import { FormField } from '../types/Form';
import { FieldWrapper } from './FieldWrapper';

interface FieldProps {
  field: FormField;
  register: UseFormRegister<any>;
  errorMessage?: string;
}

export const CheckboxField: React.FC<FieldProps> = ({ field, register, errorMessage }) => (
  <FieldWrapper label={field.label} required={field.required}>
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
  </FieldWrapper>
);
