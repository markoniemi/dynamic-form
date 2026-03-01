import {http} from './http';
import {CreateForm, Form, FormDataDto, FormListItem} from '../types/Form';

export const formClient = {
  async getAvailableForms(token: string): Promise<FormListItem[]> {
    return http.request<FormListItem[]>('/forms', {token});
  },

  async getForm(formKey: string, token: string): Promise<Form> {
    return http.request<Form>(`/forms/${formKey}`, {token});
  },

  async getAllForms(token: string): Promise<Form[]> {
    return http.request<Form[]>('/forms/all', {token});
  },

  async saveForm(form: CreateForm, token: string): Promise<Form> {
    return http.request<Form>('/forms', {
      method: 'POST',
      body: JSON.stringify(form),
      token,
    });
  },

  async updateForm(formKey: string, form: CreateForm, token: string): Promise<Form> {
    return http.request<Form>(`/forms/${formKey}`, {
      method: 'PUT',
      body: JSON.stringify(form),
      token,
    });
  },

  async deleteForm(formKey: string, token: string): Promise<void> {
    await http.request<void>(`/forms/${formKey}`, {
      method: 'DELETE',
      token,
    });
  },

  async submitForm(formKey: string, data: Record<string, unknown>, token: string): Promise<FormDataDto> {
    return http.request<FormDataDto>(`/form-data/${formKey}`, {
      method: 'POST',
      body: JSON.stringify(data),
      token,
    });
  },

  async getFormSubmissions(formKey: string, token: string): Promise<FormDataDto[]> {
    return http.request<FormDataDto[]>(`/form-data/${formKey}`, {token});
  },

  async getAllSubmissions(token: string): Promise<FormDataDto[]> {
    return http.request<FormDataDto[]>('/form-data', {token});
  },

  async getSubmissionById(id: number, token: string): Promise<FormDataDto> {
    return http.request<FormDataDto>(`/form-data/submission/${id}`, {token});
  },

  async deleteSubmission(id: number, token: string): Promise<void> {
    await http.request<void>(`/form-data/submission/${id}`, {
      method: 'DELETE',
      token,
    });
  },
};
