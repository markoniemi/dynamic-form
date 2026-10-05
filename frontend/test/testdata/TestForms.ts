import {Form} from '../../src/types/Form';
import {TestFields} from './TestFields';

export const TestForms = {
  contact: {
    formKey: 'contact',
    title: 'Contact Form',
    description: 'Please fill in your details',
    fields: [TestFields.text, TestFields.email, TestFields.checkbox],
  },
} satisfies Record<string, Form>;
