import React from 'react';
import {Form} from 'react-bootstrap';
import {useTranslation} from 'react-i18next';
import {FieldWrapper} from './FieldWrapper';
import {FieldProps, requiredRule} from './FieldProps';

export const CheckboxField: React.FC<FieldProps> = ({field, register, errorMessage}) => {
  const {t} = useTranslation();
  return (
    <FieldWrapper label={field.label} required={field.required}>
      {field.options?.map((option) => (
        <Form.Check
          key={option.value}
          type="checkbox"
          id={`${field.name}-${option.value}`}
          label={option.label}
          value={option.value}
          {...register(field.name, requiredRule(field, t))}
          isInvalid={!!errorMessage}
        />
      ))}
      {errorMessage && <div className="invalid-feedback d-block">{errorMessage}</div>}
    </FieldWrapper>
  );
};
