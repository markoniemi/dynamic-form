import React from 'react';
import {Form} from 'react-bootstrap';
import {useTranslation} from 'react-i18next';
import {FormField} from '../types/Form';
import {FieldWrapper} from './FieldWrapper';

interface ReadOnlyDynamicFormProps {
  fields: FormField[];
  data: Record<string, unknown>;
}

export const ReadOnlyDynamicForm: React.FC<ReadOnlyDynamicFormProps> = ({fields, data}) => {
  const {t} = useTranslation();

  const renderFieldValue = (field: FormField) => {
    const value = data[field.name];

    const getDisplayValue = (): string => {
      if (value === undefined || value === null || value === '') {
        return t('common.empty');
      }

      if (field.type === 'checkbox') {
        return value ? t('common.yes') : t('common.no');
      }

      if (field.type === 'select' || field.type === 'radio') {
        const option = field.options?.find((opt) => opt.value === value);
        return option?.label ?? String(value);
      }

      if (field.type === 'date' && typeof value === 'string') {
        return new Date(value).toLocaleDateString();
      }

      return String(value);
    };

    return (
      <FieldWrapper key={field.name} label={field.label} required={false} controlId={field.name}>
        <Form.Control plaintext readOnly defaultValue={getDisplayValue()}/>
      </FieldWrapper>
    );
  };

  return <>{fields.map(renderFieldValue)}</>;
};

