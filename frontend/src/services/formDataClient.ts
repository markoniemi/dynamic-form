import {http} from './http';
import {FormDataDto} from '../types/Form';

export const formDataClient = {
  async submitForm(formKey: string, data: Record<string, unknown>, token: string): Promise<FormDataDto> {
    return http.request<FormDataDto>(`/form-data/${formKey}`, {
      method: 'POST',
      body: JSON.stringify(data),
      token,
    });
  },

  async updateSubmission(id: number, data: Record<string, unknown>, token: string): Promise<FormDataDto> {
    return http.request<FormDataDto>(`/form-data/submission/${id}`, {
      method: 'PUT',
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
