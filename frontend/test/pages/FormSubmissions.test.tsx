import {render, screen, waitFor} from '@testing-library/react';
import {userEvent} from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {FormSubmissions} from '../../src/pages/FormSubmissions';
import {useAuth} from 'react-oidc-context';
import type {AuthContextProps} from 'react-oidc-context';
import type {User} from 'oidc-client-ts';
import {BrowserRouter} from 'react-router-dom';
import {formDataClient} from '../../src/services/formDataClient';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {FormDataDto} from '../../src/types/Form';

// Mock dependencies
vi.mock('react-oidc-context');
vi.mock('../../src/services/formDataClient', () => ({
  formDataClient: {
    getAllSubmissions: vi.fn(),
  },
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...(actual as Record<string, unknown>),
    useNavigate: () => mockNavigate,
  };
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {retry: false},
  },
});

const mockSubmissions: FormDataDto[] = [
  {
    id: 1,
    formKey: 'contact',
    data: {fullName: 'Jane Doe', email: 'jane@example.com'},
    submittedAt: '2024-06-01T10:00:00.000Z',
    submittedBy: 'test-user',
  },
  {
    id: 2,
    formKey: 'feedback',
    data: {message: 'Great service!'},
    submittedAt: '2024-06-02T14:30:00.000Z',
    submittedBy: 'test-user',
  },
];

function renderFormSubmissions() {
  render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <FormSubmissions/>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('FormSubmissions Component', () => {
  const mockUser = {
    access_token: 'mock-token',
    profile: {sub: 'test-user'},
  } as unknown as User;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
    vi.mocked(useAuth).mockReturnValue({
      user: mockUser,
    } as unknown as AuthContextProps);
  });

  it('renders loading spinner while fetching submissions', () => {
    vi.mocked(formDataClient.getAllSubmissions).mockImplementation(() => new Promise(() => {
    }));
    renderFormSubmissions();
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('renders the page heading', async () => {
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue(mockSubmissions);
    renderFormSubmissions();

    await waitFor(() => {
      expect(screen.getByText('submissions.title')).toBeInTheDocument();
    });
  });

  it('renders table with all submissions when data is loaded', async () => {
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue(mockSubmissions);
    renderFormSubmissions();

    await waitFor(() => {
      expect(screen.getByText('contact')).toBeInTheDocument();
      expect(screen.getByText('feedback')).toBeInTheDocument();
      expect(screen.getAllByRole('button', {name: 'submissions.table.view'})).toHaveLength(2);
      expect(screen.getAllByRole('button', {name: 'submissions.table.edit'})).toHaveLength(2);
    });
  });

  it('renders table headers correctly', async () => {
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue(mockSubmissions);
    renderFormSubmissions();

    await waitFor(() => {
      expect(screen.getByText('submissions.table.id')).toBeInTheDocument();
      expect(screen.getByText('submissions.table.formKey')).toBeInTheDocument();
      expect(screen.getByText('submissions.table.submittedAt')).toBeInTheDocument();
      expect(screen.getByText('submissions.table.submittedBy')).toBeInTheDocument();
      expect(screen.getByText('submissions.table.actions')).toBeInTheDocument();
    });
  });

  it('renders "No submissions found" alert when the list is empty', async () => {
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue([]);
    renderFormSubmissions();

    await waitFor(() => {
      expect(screen.getByText('submissions.noSubmissions')).toBeInTheDocument();
    });
  });

  it('renders error alert when fetching fails', async () => {
    vi.mocked(formDataClient.getAllSubmissions).mockRejectedValue(new Error('Failed to load submissions'));
    renderFormSubmissions();

    await waitFor(() => {
      expect(screen.getByText('Failed to load submissions')).toBeInTheDocument();
    });
  });

  it('does not render the table when an error occurs', async () => {
    vi.mocked(formDataClient.getAllSubmissions).mockRejectedValue(new Error('Server error'));
    renderFormSubmissions();

    await waitFor(() => screen.getByText('Server error'));
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('navigates to the submission detail page when "View Details" is clicked', async () => {
    const user = userEvent.setup();
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue(mockSubmissions);
    renderFormSubmissions();

    await waitFor(() => screen.getAllByRole('button', {name: 'submissions.table.view'}));

    const detailButtons = screen.getAllByRole('button', {name: 'submissions.table.view'});
    await user.click(detailButtons[0]);
    expect(mockNavigate).toHaveBeenCalledWith('/forms/submissions/1');
  });

  it('navigates to the correct submission when second "View Details" is clicked', async () => {
    const user = userEvent.setup();
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue(mockSubmissions);
    renderFormSubmissions();

    await waitFor(() => screen.getAllByRole('button', {name: 'submissions.table.view'}));

    const detailButtons = screen.getAllByRole('button', {name: 'submissions.table.view'});
    await user.click(detailButtons[1]);
    expect(mockNavigate).toHaveBeenCalledWith('/forms/submissions/2');
  });

  it('navigates to the submission edit page when "Edit" is clicked', async () => {
    const user = userEvent.setup();
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue(mockSubmissions);
    renderFormSubmissions();

    await waitFor(() => screen.getAllByRole('button', {name: 'submissions.table.edit'}));

    const editButtons = screen.getAllByRole('button', {name: 'submissions.table.edit'});
    await user.click(editButtons[0]);
    expect(mockNavigate).toHaveBeenCalledWith('/forms/contact/submissions/1/edit');
  });

  it('calls getAllSubmissions with the user token', async () => {
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue([]);
    renderFormSubmissions();

    await waitFor(() => {
      expect(formDataClient.getAllSubmissions).toHaveBeenCalledWith('mock-token');
    });
  });

  it('does not call getAllSubmissions when no token is present', () => {
    vi.mocked(useAuth).mockReturnValue({user: null} as unknown as AuthContextProps);
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue([]);
    renderFormSubmissions();

    expect(formDataClient.getAllSubmissions).not.toHaveBeenCalled();
  });
});
