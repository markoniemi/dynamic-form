import {http} from './http';
import {CreateForm, Form, FormListItem} from '../types/Form';

export const formClient = {
  async getAvailableForms(token: string): Promise<FormListItem[]> {
    return http.request<FormListItem[]>('/forms', {token});
  },

  async getForm(formKey: string, token: string): Promise<Form> {
    return http.request<Form>(`/forms/${formKey}`, {token});
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
};
