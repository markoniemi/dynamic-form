import {screen, waitFor} from '@testing-library/react';
import {userEvent} from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {FormSubmission} from '../../src/pages/FormSubmission';
import {formClient} from '../../src/services/formClient';
import {formDataClient} from '../../src/services/formDataClient';
import {renderWithProviders} from '../renderWithProviders';
import {TestFields} from '../testdata/TestFields';
import {TestForms} from '../testdata/TestForms';
import {TestSubmissions} from '../testdata/TestSubmissions';
import {mockAuth, TestUsers} from '../testdata/TestUsers';

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

function renderCreate() {
  renderWithProviders(<FormSubmission/>, {route: `/forms/${form.formKey}`, path: '/forms/:formKey'});
}

function renderEdit() {
  renderWithProviders(<FormSubmission/>, {
    route: `/forms/${form.formKey}/submissions/${submission.id}/edit`,
    path: '/forms/:formKey/submissions/:id/edit',
  });
}

describe('FormSubmission Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuth();
  });

  describe('Create Mode', () => {
    it('renders loading spinner while fetching form definition', () => {
      vi.mocked(formClient.getForm).mockImplementation(() => new Promise(() => {
      }));
      renderCreate();
      expect(screen.getByRole('status')).toBeInTheDocument();
    });

    it('renders form fields after loading', async () => {
      vi.mocked(formClient.getForm).mockResolvedValue(form);
      renderCreate();

      await waitFor(() => {
        expect(screen.getByText(form.title)).toBeInTheDocument();
        expect(screen.getByText(form.description)).toBeInTheDocument();
        expect(screen.getByPlaceholderText(TestFields.text.placeholder)).toBeInTheDocument();
        expect(screen.getByPlaceholderText(TestFields.email.placeholder)).toBeInTheDocument();
      });
    });

    it('submits form successfully and shows success alert', async () => {
      const user = userEvent.setup();
      vi.mocked(formClient.getForm).mockResolvedValue(form);
      vi.mocked(formDataClient.submitForm).mockResolvedValue(submission);
      renderCreate();

      const nameInput = await screen.findByPlaceholderText(TestFields.text.placeholder);
      await user.type(nameInput, submission.data.fullName);
      await user.type(screen.getByPlaceholderText(TestFields.email.placeholder), submission.data.email);
      await user.click(screen.getByRole('button', {name: 'form.submit'}));

      await waitFor(() => {
        expect(formDataClient.submitForm).toHaveBeenCalledWith(
          form.formKey,
          {fullName: submission.data.fullName, email: submission.data.email},
          token
        );
        expect(screen.getByText('form.success')).toBeInTheDocument();
      });
    });
  });

  describe('Edit Mode', () => {
    it('fetches submission data and populates the form', async () => {
      vi.mocked(formClient.getForm).mockResolvedValue(form);
      vi.mocked(formDataClient.getSubmissionById).mockResolvedValue(submission);
      renderEdit();

      await waitFor(() => {
        expect(screen.getByDisplayValue(submission.data.fullName)).toBeInTheDocument();
        expect(screen.getByDisplayValue(submission.data.email)).toBeInTheDocument();
      });
      expect(formDataClient.getSubmissionById).toHaveBeenCalledWith(submission.id, token);
    });

    it('updates form successfully and shows success alert', async () => {
      const user = userEvent.setup();
      vi.mocked(formClient.getForm).mockResolvedValue(form);
      vi.mocked(formDataClient.getSubmissionById).mockResolvedValue(submission);
      vi.mocked(formDataClient.updateSubmission).mockResolvedValue(submission);
      renderEdit();

      const nameInput = await screen.findByDisplayValue(submission.data.fullName);
      await user.clear(nameInput);
      await user.type(nameInput, 'Jane Doe Updated');
      await user.click(screen.getByRole('button', {name: 'form.submit'}));

      await waitFor(() => {
        expect(formDataClient.updateSubmission).toHaveBeenCalledWith(
          submission.id,
          {...submission.data, fullName: 'Jane Doe Updated'},
          token
        );
        expect(screen.getByText('form.updateSuccess')).toBeInTheDocument();
      });
    });

    it('shows "Updating..." text on the button while mutation is pending', async () => {
      const user = userEvent.setup();
      vi.mocked(formClient.getForm).mockResolvedValue(form);
      vi.mocked(formDataClient.getSubmissionById).mockResolvedValue(submission);
      vi.mocked(formDataClient.updateSubmission).mockImplementation(() => new Promise(() => {
      }));
      renderEdit();

      await screen.findByDisplayValue(submission.data.fullName);
      await user.click(screen.getByRole('button', {name: 'form.submit'}));

      await waitFor(() => {
        expect(screen.getByRole('button', {name: 'form.submitting'})).toBeInTheDocument();
      });
    });
  });
});
