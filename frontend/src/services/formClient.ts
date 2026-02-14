import { http } from './http';
import { FormDefinition, FormDataDto, CreateFormDefinition } from '../types/Form';

export const formClient = {
  async getAvailableForms(): Promise<string[]> {
    return http.request<string[]>('/forms', {});
  },

  async getFormDefinition(formKey: string): Promise<FormDefinition> {
    return http.request<FormDefinition>(`/forms/${formKey}`, {});
  },

  async getAllFormDefinitions(token: string): Promise<FormDefinition[]> {
    return http.request<FormDefinition[]>('/forms/all', { token });
  },

  async saveFormDefinition(formDefinition: CreateFormDefinition, token: string): Promise<FormDefinition> {
    return http.request<FormDefinition>('/forms', {
      method: 'POST',
      body: JSON.stringify(formDefinition),
      token,
    });
  },

  async updateFormDefinition(formKey: string, formDefinition: CreateFormDefinition, token: string): Promise<FormDefinition> {
    return http.request<FormDefinition>(`/forms/${formKey}`, {
      method: 'PUT',
      body: JSON.stringify(formDefinition),
      token,
    });
  },

  async deleteFormDefinition(formKey: string, token: string): Promise<void> {
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
    return http.request<FormDataDto[]>(`/form-data/${formKey}`, { token });
  },

  async getAllSubmissions(token: string): Promise<FormDataDto[]> {
    return http.request<FormDataDto[]>('/form-data', { token });
  },

  async getSubmissionById(id: number, token: string): Promise<FormDataDto> {
    return http.request<FormDataDto>(`/form-data/submission/${id}`, { token });
  },

  async deleteSubmission(id: number, token: string): Promise<void> {
    await http.request<void>(`/form-data/submission/${id}`, {
      method: 'DELETE',
      token,
    });
  },
};
