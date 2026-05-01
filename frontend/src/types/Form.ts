export interface FormField {
  name: string;
  label: string;
  type: 'text' | 'email' | 'tel' | 'number' | 'date' | 'textarea' | 'select' | 'radio' | 'checkbox';
  required: boolean;
  placeholder?: string;
  options?: FieldOption[];
}

export interface FieldOption {
  value: string;
  label: string;
}

export interface Form {
  id?: number;
  formKey?: string;
  title: string;
  description: string;
  fields: FormField[];
}

export interface CreateForm {
  formKey: string;
  title: string;
  description: string;
  fields: FormField[];
}

export interface FormListItem {
  readonly formKey: string;
  readonly title: string;
}

export interface FormDataDto {
  readonly id: number;
  readonly formKey: string;
  readonly data: Record<string, unknown>;
  readonly submittedAt: string;
  readonly submittedBy: string;
}

export type FormValues = Record<string, string | string[] | boolean>;

export const FIELD_TYPES = [
  {value: 'text', label: 'Text'},
  {value: 'email', label: 'Email'},
  {value: 'tel', label: 'Phone'},
  {value: 'number', label: 'Number'},
  {value: 'date', label: 'Date'},
  {value: 'textarea', label: 'Text Area'},
  {value: 'select', label: 'Dropdown'},
  {value: 'radio', label: 'Radio Buttons'},
  {value: 'checkbox', label: 'Checkbox'},
] as const satisfies ReadonlyArray<{readonly value: FormField['type']; readonly label: string}>;
