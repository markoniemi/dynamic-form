import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { FormSubmission } from '../../src/pages/FormSubmission';
import { useAuth } from 'react-oidc-context';
import { BrowserRouter, useParams } from 'react-router-dom';
import { formClient } from '../../src/services/formClient';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Form } from '../../src/types/Form';

// Mock dependencies
vi.mock('react-oidc-context');
vi.mock('../../src/services/formClient', () => ({
  formClient: {
    getForm: vi.fn(),
    submitForm: vi.fn(),
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
    queries: { retry: false },
    mutations: { retry: false },
  },
});

const mockForm: Form = {
  title: 'Contact Form',
  description: 'Please fill in your details',
  fields: [
    { name: 'fullName', label: 'Full Name', type: 'text', required: true, placeholder: 'Enter your name' },
    { name: 'email', label: 'Email', type: 'email', required: true, placeholder: 'Enter your email' },
  ],
};

function renderFormSubmission() {
  render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <FormSubmission />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('FormSubmission Component', () => {
  const mockUser = {
    access_token: 'mock-token',
    profile: { sub: 'test-user' },
  };

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
    (useParams as any).mockReturnValue({ formKey: 'contact' });
    (useAuth as any).mockReturnValue({
      isAuthenticated: true,
      user: mockUser,
      signinRedirect: mockSigninRedirect,
    });
  });

  it('renders loading spinner while fetching form definition', () => {
    vi.mocked(formClient.getForm).mockImplementation(() => new Promise(() => {}));
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

  it('renders error alert when form definition fails to load', async () => {
    vi.mocked(formClient.getForm).mockRejectedValue(new Error('Form not found'));
    renderFormSubmission();

    await waitFor(() => {
      expect(screen.getByText('Form not found')).toBeInTheDocument();
      expect(screen.getByText('Back to Forms')).toBeInTheDocument();
    });
  });

  it('navigates to /forms when "Back to Forms" is clicked on error', async () => {
    vi.mocked(formClient.getForm).mockRejectedValue(new Error('Form not found'));
    renderFormSubmission();

    await waitFor(() => screen.getByText('Back to Forms'));
    fireEvent.click(screen.getByText('Back to Forms'));
    expect(mockNavigate).toHaveBeenCalledWith('/forms');
  });

  it('renders warning alert and disables submit when user is not authenticated', async () => {
    (useAuth as any).mockReturnValue({
      isAuthenticated: false,
      user: null,
      signinRedirect: mockSigninRedirect,
    });
    vi.mocked(formClient.getForm).mockResolvedValue(mockForm);
    renderFormSubmission();

    await waitFor(() => {
      expect(screen.getByText(/You must be logged in to submit this form/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /submit/i })).toBeDisabled();
    });
  });

  it('calls signinRedirect when login link is clicked while unauthenticated', async () => {
    (useAuth as any).mockReturnValue({
      isAuthenticated: false,
      user: null,
      signinRedirect: mockSigninRedirect,
    });
    vi.mocked(formClient.getForm).mockResolvedValue(mockForm);
    renderFormSubmission();

    await waitFor(() => screen.getByText('Click here to log in'));
    fireEvent.click(screen.getByText('Click here to log in'));
    expect(mockSigninRedirect).toHaveBeenCalled();
  });

  it('submits form successfully and shows success alert', async () => {
    vi.mocked(formClient.getForm).mockResolvedValue(mockForm);
    vi.mocked(formClient.submitForm).mockResolvedValue({
      id: 1,
      formKey: 'contact',
      data: { fullName: 'Jane Doe', email: 'jane@example.com' },
      submittedAt: new Date().toISOString(),
      submittedBy: 'test-user',
    });
    renderFormSubmission();

    await waitFor(() => screen.getByPlaceholderText('Enter your name'));

    fireEvent.change(screen.getByPlaceholderText('Enter your name'), {
      target: { value: 'Jane Doe' },
    });
    fireEvent.change(screen.getByPlaceholderText('Enter your email'), {
      target: { value: 'jane@example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => {
      expect(formClient.submitForm).toHaveBeenCalledWith(
        'contact',
        { fullName: 'Jane Doe', email: 'jane@example.com' },
        'mock-token'
      );
      expect(screen.getByText(/Form submitted successfully/i)).toBeInTheDocument();
    });
  });

  it('shows mutation error alert when form submission fails', async () => {
    vi.mocked(formClient.getForm).mockResolvedValue(mockForm);
    vi.mocked(formClient.submitForm).mockRejectedValue(new Error('Submission failed'));
    renderFormSubmission();

    await waitFor(() => screen.getByPlaceholderText('Enter your name'));

    fireEvent.change(screen.getByPlaceholderText('Enter your name'), {
      target: { value: 'Jane Doe' },
    });
    fireEvent.change(screen.getByPlaceholderText('Enter your email'), {
      target: { value: 'jane@example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => {
      expect(screen.getByText('Submission failed')).toBeInTheDocument();
    });
  });

  it('shows "Submitting..." text on the button while mutation is pending', async () => {
    vi.mocked(formClient.getForm).mockResolvedValue(mockForm);
    vi.mocked(formClient.submitForm).mockImplementation(() => new Promise(() => {}));
    renderFormSubmission();

    await waitFor(() => screen.getByPlaceholderText('Enter your name'));

    fireEvent.change(screen.getByPlaceholderText('Enter your name'), {
      target: { value: 'Jane Doe' },
    });
    fireEvent.change(screen.getByPlaceholderText('Enter your email'), {
      target: { value: 'jane@example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /submitting/i })).toBeInTheDocument();
    });
  });

  it('navigates to /submissions when Cancel is clicked', async () => {
    vi.mocked(formClient.getForm).mockResolvedValue(mockForm);
    renderFormSubmission();

    await waitFor(() => screen.getByText('Cancel'));
    fireEvent.click(screen.getByText('Cancel'));
    expect(mockNavigate).toHaveBeenCalledWith('/submissions');
  });
});
