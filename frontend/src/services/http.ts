const API_BASE_URL = '/api';

export interface RequestOptions extends RequestInit {
  token?: string;
}

export interface ValidationError {
  field: string | null;
  message: string;
  code: string;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errors?: ValidationError[];
}

export class ApiValidationError extends Error {
  constructor(
    message: string,
    public readonly validationErrors: ValidationError[]
  ) {
    super(message);
    this.name = 'ApiValidationError';
  }
}

export const http = {
  async request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
    const {token, ...fetchOptions} = options;

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
        const pd = (await response.json()) as ProblemDetail;
        if (pd.errors?.length) {
          throw new ApiValidationError(
            pd.title ?? pd.detail ?? 'Validation failed',
            pd.errors
          );
        }
        throw new Error(pd.detail ?? pd.title ?? `API request failed: ${response.statusText}`);
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
