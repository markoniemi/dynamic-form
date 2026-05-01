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

export type FormValues = Record<string, string | string[] | boolean>
