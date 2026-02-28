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
  formKey: string;
  title: string;
}

export interface FormDataDto {
  id: number;
  formKey: string;
  data: Record<string, any>;
  submittedAt: string;
  submittedBy: string;
}

export type FormValues = Record<string, string | string[] | boolean>