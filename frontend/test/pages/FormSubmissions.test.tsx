import {screen, waitFor} from '@testing-library/react';
import {userEvent} from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {FormSubmissions} from '../../src/pages/FormSubmissions';
import {formDataClient} from '../../src/services/formDataClient';
import {renderWithProviders} from '../renderWithProviders';
import {TestSubmissions} from '../testdata/TestSubmissions';
import {mockAuth, TestUsers} from '../testdata/TestUsers';

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

const {contact, feedback} = TestSubmissions;
const submissions = [contact, feedback];

function renderFormSubmissions() {
  renderWithProviders(<FormSubmissions/>);
}

describe('FormSubmissions Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuth();
  });

  it('renders loading spinner while fetching submissions', () => {
    vi.mocked(formDataClient.getAllSubmissions).mockImplementation(() => new Promise(() => {
    }));
    renderFormSubmissions();
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('renders the page heading', async () => {
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue(submissions);
    renderFormSubmissions();

    await waitFor(() => {
      expect(screen.getByText('submissions.title')).toBeInTheDocument();
    });
  });

  it('renders table with all submissions when data is loaded', async () => {
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue(submissions);
    renderFormSubmissions();

    await waitFor(() => {
      expect(screen.getByText(contact.formKey)).toBeInTheDocument();
      expect(screen.getByText(feedback.formKey)).toBeInTheDocument();
      expect(screen.getAllByRole('button', {name: 'submissions.table.view'})).toHaveLength(submissions.length);
      expect(screen.getAllByRole('button', {name: 'submissions.table.edit'})).toHaveLength(submissions.length);
    });
  });

  it('renders table headers correctly', async () => {
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue(submissions);
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
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue(submissions);
    renderFormSubmissions();

    const detailButtons = await screen.findAllByRole('button', {name: 'submissions.table.view'});
    await user.click(detailButtons[0]);
    expect(mockNavigate).toHaveBeenCalledWith(`/forms/submissions/${contact.id}`);
  });

  it('navigates to the correct submission when second "View Details" is clicked', async () => {
    const user = userEvent.setup();
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue(submissions);
    renderFormSubmissions();

    const detailButtons = await screen.findAllByRole('button', {name: 'submissions.table.view'});
    await user.click(detailButtons[1]);
    expect(mockNavigate).toHaveBeenCalledWith(`/forms/submissions/${feedback.id}`);
  });

  it('navigates to the submission edit page when "Edit" is clicked', async () => {
    const user = userEvent.setup();
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue(submissions);
    renderFormSubmissions();

    const editButtons = await screen.findAllByRole('button', {name: 'submissions.table.edit'});
    await user.click(editButtons[0]);
    expect(mockNavigate).toHaveBeenCalledWith(`/forms/${contact.formKey}/submissions/${contact.id}/edit`);
  });

  it('calls getAllSubmissions with the user token', async () => {
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue([]);
    renderFormSubmissions();

    await waitFor(() => {
      expect(formDataClient.getAllSubmissions).toHaveBeenCalledWith(TestUsers.user.access_token);
    });
  });

  it('does not call getAllSubmissions when no token is present', () => {
    mockAuth({user: null});
    vi.mocked(formDataClient.getAllSubmissions).mockResolvedValue([]);
    renderFormSubmissions();

    expect(formDataClient.getAllSubmissions).not.toHaveBeenCalled();
  });
});
