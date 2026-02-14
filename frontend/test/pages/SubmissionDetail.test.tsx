import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SubmissionDetail } from '../../src/pages/SubmissionDetail';
import { useAuth } from 'react-oidc-context';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { formClient } from '../../src/services/formClient';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { FormDataDto, FormDefinition } from '../../src/types/Form';

vi.mock('react-oidc-context');
vi.mock('../../src/services/formClient', () => ({
  formClient: {
    getSubmissionById: vi.fn(),
    getFormDefinition: vi.fn(),
  },
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false },
  },
});

const mockSubmission: FormDataDto = {
  id: 1,
  formKey: 'contact',
  data: { fullName: 'Jane Doe', email: 'jane@example.com', newsletter: true },
  submittedAt: '2024-06-01T10:00:00.000Z',
};

const mockFormDefinition: FormDefinition = {
  title: 'Contact Form',
  description: 'Please fill out your contact information',
  fields: [
    { name: 'fullName', label: 'Full Name', type: 'text', required: true },
    { name: 'email', label: 'Email', type: 'email', required: true },
    { name: 'newsletter', label: 'Subscribe to Newsletter', type: 'checkbox', required: false },
  ],
};

function renderSubmissionDetail(submissionId: string = '1') {
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/forms/submissions/${submissionId}`]}>
        <Routes>
          <Route path="/forms/submissions/:id" element={<SubmissionDetail />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('SubmissionDetail Component', () => {
  const mockUser = {
    access_token: 'mock-token',
    profile: { sub: 'test-user' },
  };

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
    vi.mocked(useAuth).mockReturnValue({
      user: mockUser,
    } as ReturnType<typeof useAuth>);
  });

  it('renders loading spinner while fetching data', () => {
    vi.mocked(formClient.getSubmissionById).mockImplementation(() => new Promise(() => {}));
    renderSubmissionDetail();
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('renders error alert when fetching submission fails', async () => {
    vi.mocked(formClient.getSubmissionById).mockRejectedValue(new Error('Failed to load submission'));
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByText('Failed to load submission')).toBeInTheDocument();
    });
  });

  it('renders back button when error occurs', async () => {
    vi.mocked(formClient.getSubmissionById).mockRejectedValue(new Error('Error'));
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /back to submissions/i })).toBeInTheDocument();
    });
  });

  it('renders submission details when data is loaded', async () => {
    vi.mocked(formClient.getSubmissionById).mockResolvedValue(mockSubmission);
    vi.mocked(formClient.getFormDefinition).mockResolvedValue(mockFormDefinition);
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByText('Contact Form')).toBeInTheDocument();
      expect(screen.getByText('Please fill out your contact information')).toBeInTheDocument();
    });
  });

  it('renders submission metadata', async () => {
    vi.mocked(formClient.getSubmissionById).mockResolvedValue(mockSubmission);
    vi.mocked(formClient.getFormDefinition).mockResolvedValue(mockFormDefinition);
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByText(/Submission ID:/)).toBeInTheDocument();
      expect(screen.getByText(/Form Key:/)).toBeInTheDocument();
      expect(screen.getByText(/Submitted At:/)).toBeInTheDocument();
    });
  });

  it('renders field labels from form definition', async () => {
    vi.mocked(formClient.getSubmissionById).mockResolvedValue(mockSubmission);
    vi.mocked(formClient.getFormDefinition).mockResolvedValue(mockFormDefinition);
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByText('Full Name')).toBeInTheDocument();
      expect(screen.getByText('Email')).toBeInTheDocument();
      expect(screen.getByText('Subscribe to Newsletter')).toBeInTheDocument();
    });
  });

  it('renders submitted field values', async () => {
    vi.mocked(formClient.getSubmissionById).mockResolvedValue(mockSubmission);
    vi.mocked(formClient.getFormDefinition).mockResolvedValue(mockFormDefinition);
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByDisplayValue('Jane Doe')).toBeInTheDocument();
      expect(screen.getByDisplayValue('jane@example.com')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Yes')).toBeInTheDocument();
    });
  });

  it('navigates back to submissions when back button is clicked', async () => {
    vi.mocked(formClient.getSubmissionById).mockResolvedValue(mockSubmission);
    vi.mocked(formClient.getFormDefinition).mockResolvedValue(mockFormDefinition);
    renderSubmissionDetail();

    await waitFor(() => screen.getByRole('button', { name: /back to submissions/i }));

    const backButton = screen.getByRole('button', { name: /back to submissions/i });
    fireEvent.click(backButton);
    expect(mockNavigate).toHaveBeenCalledWith('/submissions');
  });

  it('calls getSubmissionById with correct parameters', async () => {
    vi.mocked(formClient.getSubmissionById).mockResolvedValue(mockSubmission);
    vi.mocked(formClient.getFormDefinition).mockResolvedValue(mockFormDefinition);
    renderSubmissionDetail('42');

    await waitFor(() => {
      expect(formClient.getSubmissionById).toHaveBeenCalledWith(42, 'mock-token');
    });
  });

  it('calls getFormDefinition with the form key from submission', async () => {
    vi.mocked(formClient.getSubmissionById).mockResolvedValue(mockSubmission);
    vi.mocked(formClient.getFormDefinition).mockResolvedValue(mockFormDefinition);
    renderSubmissionDetail();

    await waitFor(() => {
      expect(formClient.getFormDefinition).toHaveBeenCalledWith('contact');
    });
  });

  it('renders "Submission not found" when submission is null', async () => {
    vi.mocked(formClient.getSubmissionById).mockResolvedValue(null as unknown as FormDataDto);
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByText('Submission not found.')).toBeInTheDocument();
    });
  });

  it('does not call getSubmissionById when no token is present', () => {
    vi.mocked(useAuth).mockReturnValue({ user: null } as ReturnType<typeof useAuth>);
    vi.mocked(formClient.getSubmissionById).mockResolvedValue(mockSubmission);
    renderSubmissionDetail();

    expect(formClient.getSubmissionById).not.toHaveBeenCalled();
  });
});

