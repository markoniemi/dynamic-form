import React from 'react';
import {Form} from 'react-bootstrap';
import {useTranslation} from 'react-i18next';
import {FieldWrapper} from './FieldWrapper';
import {FieldProps, requiredRule} from './FieldProps';

export const TextField: React.FC<FieldProps> = ({field, register, errorMessage}) => {
  const {t} = useTranslation();
  return (
    <FieldWrapper label={field.label} required={field.required} controlId={field.name}>
      <Form.Control
        type={field.type}
        placeholder={field.placeholder}
        {...register(field.name, requiredRule(field, t))}
        isInvalid={!!errorMessage}
      />
      <Form.Control.Feedback type="invalid">{errorMessage}</Form.Control.Feedback>
    </FieldWrapper>
  );
};
