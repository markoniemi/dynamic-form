const API_BASE_URL = '/api';

export interface RequestOptions extends RequestInit {
  token?: string;
}

export interface ErrorDto {
  message: string;
  errors?: Record<string, string>;
}

export const http = {
  async request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
    const { token, ...fetchOptions } = options;

    const headers = new Headers(fetchOptions.headers);

    if (!headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }

    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...fetchOptions,
      headers,
    });

    if (!response.ok) {
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.indexOf('application/json') !== -1) {
        const errorData = (await response.json()) as ErrorDto;
        throw new Error(errorData.message || `API request failed: ${response.statusText}`);
      }
      throw new Error(`API request failed: ${response.statusText}`);
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.indexOf('application/json') !== -1) {
      return response.json();
    }

    return null as unknown as T;
  },
};
