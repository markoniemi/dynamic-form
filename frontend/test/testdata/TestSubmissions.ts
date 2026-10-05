import {FormDataDto} from '../../src/types/Form';
import {TestForms} from './TestForms';
import {TestUsers} from './TestUsers';

export const TestSubmissions = {
  contact: {
    id: 1,
    formKey: TestForms.contact.formKey,
    data: {fullName: 'Jane Doe', email: 'jane@example.com', newsletter: true},
    submittedAt: '2024-06-01T10:00:00.000Z',
    submittedBy: TestUsers.user.profile.sub,
  },
  feedback: {
    id: 2,
    formKey: 'feedback',
    data: {message: 'Great service!'},
    submittedAt: '2024-06-02T14:30:00.000Z',
    submittedBy: TestUsers.user.profile.sub,
  },
} satisfies Record<string, FormDataDto>;
