import React from 'react';
import {Form} from 'react-bootstrap';
import {UseFormRegister} from 'react-hook-form';
import {FormField, FormValues} from '../types/Form';
import {FieldWrapper} from './FieldWrapper';

interface FieldProps {
  field: FormField;
  register: UseFormRegister<FormValues>;
  errorMessage?: string;
}

export const RadioField: React.FC<FieldProps> = ({field, register, errorMessage}) => (
  <FieldWrapper label={field.label} required={field.required}>
    {field.options?.map((option) => (
      <Form.Check
        key={option.value}
        type="radio"
        id={`${field.name}-${option.value}`}
        label={option.label}
        value={option.value}
        {...register(field.name, {required: field.required ? `${field.label} is required` : false})}
        isInvalid={!!errorMessage}
      />
    ))}
    {errorMessage && <div className="invalid-feedback d-block">{errorMessage}</div>}
  </FieldWrapper>
);
