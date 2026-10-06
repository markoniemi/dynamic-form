import React from 'react';
import {FieldErrors, UseFormRegister} from 'react-hook-form';
import {FormField, FormValues} from '../types/Form';
import {FieldProps} from './FieldProps';
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

// Record over every field type makes adding a type without a component a compile error
const FIELD_COMPONENTS: Record<FormField['type'], React.FC<FieldProps>> = {
  text: TextField,
  email: TextField,
  tel: TextField,
  number: TextField,
  date: TextField,
  textarea: TextAreaField,
  select: SelectField,
  radio: RadioField,
  checkbox: CheckboxField,
};

export const DynamicForm: React.FC<DynamicFormProps> = ({fields, register, errors}) => (
  <>
    {fields.map((field) => {
      const Field = FIELD_COMPONENTS[field.type];
      const msg = errors[field.name]?.message;
      return (
        <Field
          key={field.name}
          field={field}
          register={register}
          errorMessage={typeof msg === 'string' ? msg : undefined}
        />
      );
    })}
  </>
);
