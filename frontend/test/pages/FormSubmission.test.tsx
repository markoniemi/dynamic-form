import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {FormSubmission} from '../../src/pages/FormSubmission';
import {useAuth} from 'react-oidc-context';
import type {AuthContextProps, User} from 'react-oidc-context';
import {BrowserRouter, useParams} from 'react-router-dom';
import {formClient} from '../../src/services/formClient';
import {formDataClient} from '../../src/services/formDataClient';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {Form, FormDataDto} from '../../src/types/Form';

// Mock dependencies
vi.mock('react-oidc-context');
vi.mock('../../src/services/formClient', () => ({
  formClient: {
    getForm: vi.fn(),
  },
}));
vi.mock('../../src/services/formDataClient', () => ({
  formDataClient: {
    submitForm: vi.fn(),
    getSubmissionById: vi.fn(),
    updateSubmission: vi.fn(),
  },
}));

const mockNavigate = vi.fn();
const mockSigninRedirect = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: vi.fn(),
  };
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {retry: false},
    mutations: {retry: false},
  },
});

const mockForm: Form = {
  title: 'Contact Form',
  description: 'Please fill in your details',
  fields: [
    {
      name: 'fullName',
      label: 'Full Name',
      type: 'text',
      required: true,
      placeholder: 'Enter your name'
    },
    {name: 'email', label: 'Email', type: 'email', required: true, placeholder: 'Enter your email'},
  ],
};

const mockSubmission: FormDataDto = {
  id: 1,
  formKey: 'contact',
  data: {fullName: 'Jane Doe', email: 'jane@example.com'},
  submittedAt: '2024-06-01T10:00:00.000Z',
  submittedBy: 'test-user',
};

function renderFormSubmission() {
  render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <FormSubmission/>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('FormSubmission Component', () => {
  const mockUser = {
    access_token: 'mock-token',
    profile: {sub: 'test-user'},
  } as unknown as User;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
    vi.mocked(useParams).mockReturnValue({formKey: 'contact'});
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      user: mockUser,
      signinRedirect: mockSigninRedirect,
    } as unknown as AuthContextProps);
  });

  describe('Create Mode', () => {
    it('renders loading spinner while fetching form definition', () => {
      vi.mocked(formClient.getForm).mockImplementation(() => new Promise(() => {
      }));
      renderFormSubmission();
      expect(screen.getByRole('status')).toBeInTheDocument();
    });

    it('renders form fields after loading', async () => {
      vi.mocked(formClient.getForm).mockResolvedValue(mockForm);
      renderFormSubmission();

      await waitFor(() => {
        expect(screen.getByText('Contact Form')).toBeInTheDocument();
        expect(screen.getByText('Please fill in your details')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Enter your name')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Enter your email')).toBeInTheDocument();
      });
    });

    it('submits form successfully and shows success alert', async () => {
      vi.mocked(formClient.getForm).mockResolvedValue(mockForm);
      vi.mocked(formDataClient.submitForm).mockResolvedValue(mockSubmission);
      renderFormSubmission();

      await waitFor(() => screen.getByPlaceholderText('Enter your name'));

      fireEvent.change(screen.getByPlaceholderText('Enter your name'), {
        target: {value: 'Jane Doe'},
      });
      fireEvent.change(screen.getByPlaceholderText('Enter your email'), {
        target: {value: 'jane@example.com'},
      });
      fireEvent.click(screen.getByRole('button', {name: 'form.submit'}));

      await waitFor(() => {
        expect(formDataClient.submitForm).toHaveBeenCalledWith(
          'contact',
          {fullName: 'Jane Doe', email: 'jane@example.com'},
          'mock-token'
        );
        expect(screen.getByText('form.success')).toBeInTheDocument();
      });
    });
  });

  describe('Edit Mode', () => {
    beforeEach(() => {
      vi.mocked(useParams).mockReturnValue({formKey: 'contact', id: '1'});
    });

    it('fetches submission data and populates the form', async () => {
      vi.mocked(formClient.getForm).mockResolvedValue(mockForm);
      vi.mocked(formDataClient.getSubmissionById).mockResolvedValue(mockSubmission);
      renderFormSubmission();

      await waitFor(() => {
        expect(screen.getByDisplayValue('Jane Doe')).toBeInTheDocument();
        expect(screen.getByDisplayValue('jane@example.com')).toBeInTheDocument();
      });
    });

    it('updates form successfully and shows success alert', async () => {
      vi.mocked(formClient.getForm).mockResolvedValue(mockForm);
      vi.mocked(formDataClient.getSubmissionById).mockResolvedValue(mockSubmission);
      vi.mocked(formDataClient.updateSubmission).mockResolvedValue(mockSubmission);
      renderFormSubmission();

      await waitFor(() => screen.getByDisplayValue('Jane Doe'));

      fireEvent.change(screen.getByDisplayValue('Jane Doe'), {
        target: {value: 'Jane Doe Updated'},
      });
      fireEvent.click(screen.getByRole('button', {name: 'form.submit'}));

      await waitFor(() => {
        expect(formDataClient.updateSubmission).toHaveBeenCalledWith(
          1,
          {fullName: 'Jane Doe Updated', email: 'jane@example.com'},
          'mock-token'
        );
        expect(screen.getByText('form.updateSuccess')).toBeInTheDocument();
      });
    });

    it('shows "Updating..." text on the button while mutation is pending', async () => {
      vi.mocked(formClient.getForm).mockResolvedValue(mockForm);
      vi.mocked(formDataClient.getSubmissionById).mockResolvedValue(mockSubmission);
      vi.mocked(formDataClient.updateSubmission).mockImplementation(() => new Promise(() => {
      }));
      renderFormSubmission();

      await waitFor(() => screen.getByDisplayValue('Jane Doe'));

      fireEvent.click(screen.getByRole('button', {name: 'form.submit'}));

      await waitFor(() => {
        expect(screen.getByRole('button', {name: 'form.submitting'})).toBeInTheDocument();
      });
    });
  });
});
