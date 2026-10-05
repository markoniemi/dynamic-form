import {screen, waitFor} from '@testing-library/react';
import {userEvent} from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {SubmissionDetail} from '../../src/pages/SubmissionDetail';
import {formClient} from '../../src/services/formClient';
import {formDataClient} from '../../src/services/formDataClient';
import {FormDataDto} from '../../src/types/Form';
import {renderWithProviders} from '../renderWithProviders';
import {TestForms} from '../testdata/TestForms';
import {TestSubmissions} from '../testdata/TestSubmissions';
import {mockAuth, TestUsers} from '../testdata/TestUsers';

vi.mock('react-oidc-context');
vi.mock('../../src/services/formClient', () => ({
  formClient: {
    getForm: vi.fn(),
  },
}));
vi.mock('../../src/services/formDataClient', () => ({
  formDataClient: {
    getSubmissionById: vi.fn(),
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

const form = TestForms.contact;
const submission = TestSubmissions.contact;
const token = TestUsers.user.access_token;

function renderSubmissionDetail(submissionId: number = submission.id) {
  renderWithProviders(<SubmissionDetail/>, {
    route: `/forms/submissions/${submissionId}`,
    path: '/forms/submissions/:id',
  });
}

function mockLoaded() {
  vi.mocked(formDataClient.getSubmissionById).mockResolvedValue(submission);
  vi.mocked(formClient.getForm).mockResolvedValue(form);
}

describe('SubmissionDetail Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuth();
  });

  it('renders loading spinner while fetching data', () => {
    vi.mocked(formDataClient.getSubmissionById).mockImplementation(() => new Promise(() => {
    }));
    renderSubmissionDetail();
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('renders error alert when fetching submission fails', async () => {
    vi.mocked(formDataClient.getSubmissionById).mockRejectedValue(new Error('Failed to load submission'));
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByText('Failed to load submission')).toBeInTheDocument();
    });
  });

  it('renders back button when error occurs', async () => {
    vi.mocked(formDataClient.getSubmissionById).mockRejectedValue(new Error('Error'));
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByRole('button', {name: "submissionDetail.back"})).toBeInTheDocument();
    });
  });

  it('renders submission details when data is loaded', async () => {
    mockLoaded();
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByText(form.title)).toBeInTheDocument();
      expect(screen.getByText(form.description)).toBeInTheDocument();
    });
  });

  it('renders submission metadata', async () => {
    mockLoaded();
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByText("submissionDetail.id:")).toBeInTheDocument();
      expect(screen.getByText("submissionDetail.formKey:")).toBeInTheDocument();
      expect(screen.getByText("submissionDetail.submittedAt:")).toBeInTheDocument();
    });
  });

  it('renders field labels from form definition', async () => {
    mockLoaded();
    renderSubmissionDetail();

    await waitFor(() => {
      form.fields.forEach(({label}) => expect(screen.getByText(label)).toBeInTheDocument());
    });
  });

  it('renders submitted field values', async () => {
    mockLoaded();
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByDisplayValue(submission.data.fullName)).toBeInTheDocument();
      expect(screen.getByDisplayValue(submission.data.email)).toBeInTheDocument();
      expect(screen.getByDisplayValue('common.yes')).toBeInTheDocument();
    });
  });

  it('navigates back to submissions when back button is clicked', async () => {
    const user = userEvent.setup();
    mockLoaded();
    renderSubmissionDetail();

    const backButton = await screen.findByRole('button', {name: 'submissionDetail.back'});
    await user.click(backButton);
    expect(mockNavigate).toHaveBeenCalledWith('/submissions');
  });

  it('calls getSubmissionById with correct parameters', async () => {
    mockLoaded();
    renderSubmissionDetail(42);

    await waitFor(() => {
      expect(formDataClient.getSubmissionById).toHaveBeenCalledWith(42, token);
    });
  });

  it('calls getForm with the form key from submission', async () => {
    mockLoaded();
    renderSubmissionDetail();

    await waitFor(() => {
      expect(formClient.getForm).toHaveBeenCalledWith(submission.formKey, token);
    });
  });

  it('renders "Submission not found" when submission is null', async () => {
    vi.mocked(formDataClient.getSubmissionById).mockResolvedValue(null as unknown as FormDataDto);
    renderSubmissionDetail();

    await waitFor(() => {
      expect(screen.getByText('submissionDetail.notFound')).toBeInTheDocument();
    });
  });

  it('does not call getSubmissionById when no token is present', () => {
    mockAuth({user: null});
    vi.mocked(formDataClient.getSubmissionById).mockResolvedValue(submission);
    renderSubmissionDetail();

    expect(formDataClient.getSubmissionById).not.toHaveBeenCalled();
  });
});
