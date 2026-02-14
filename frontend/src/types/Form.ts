export interface FormField {
  name: string;
  label: string;
  type: 'text' | 'email' | 'tel' | 'number' | 'date' | 'textarea' | 'select' | 'radio' | 'checkbox';
  required: boolean;
  placeholder?: string;
  options?: FormFieldOption[];
}

export interface FormFieldOption {
  value: string;
  label: string;
}

export interface FormDefinition {
  title: string;
  description: string;
  fields: FormField[];
}

export interface FormDataDto {
  id: number;
  formKey: string;
  data: Record<string, any>;
  submittedAt: string;
  submittedBy: string;
}
