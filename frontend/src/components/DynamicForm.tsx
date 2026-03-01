import React from 'react';
import {FieldErrors, UseFormRegister} from 'react-hook-form';
import {FormField, FormValues} from '../types/Form';
import {TextField} from './TextField';
import {TextAreaField} from './TextAreaField';
import {SelectField} from './SelectField';
import {RadioField} from './RadioField';
import {CheckboxField} from './CheckboxField';

interface DynamicFormProps {
  fields: FormField[];
  register: UseFormRegister<FormValues>;
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
        return <TextField key={field.name} field={field} register={register}
                          errorMessage={errorMessage}/>;

      case 'textarea':
        return <TextAreaField key={field.name} field={field} register={register}
                              errorMessage={errorMessage}/>;

      case 'select':
        return <SelectField key={field.name} field={field} register={register}
                            errorMessage={errorMessage}/>;

      case 'radio':
        return <RadioField key={field.name} field={field} register={register}
                           errorMessage={errorMessage}/>;

      case 'checkbox':
        return <CheckboxField key={field.name} field={field} register={register}
                              errorMessage={errorMessage}/>;

      default:
        return null;
    }
  };

  return <>{fields.map(renderField)}</>;
};
