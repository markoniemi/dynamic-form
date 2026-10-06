import {RegisterOptions, UseFormRegister} from 'react-hook-form';
import {TFunction} from 'i18next';
import {FormField, FormValues} from '../types/Form';

/** Props shared by all field components rendered by DynamicForm. */
export interface FieldProps {
  field: FormField;
  register: UseFormRegister<FormValues>;
  errorMessage?: string;
}

/** Register options that make a required field show a translated "<label> is required" error. */
export function requiredRule(field: FormField, t: TFunction): RegisterOptions<FormValues> {
  return {required: field.required ? t('form.required', {label: field.label}) : false};
}
