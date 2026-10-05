import {FormField} from '../../src/types/Form';

/** One field per field type; spread to vary, e.g. {...TestFields.text, required: false}. */
export const TestFields = {
  text: {name: 'fullName', label: 'Full Name', type: 'text', required: true, placeholder: 'Enter your name'},
  email: {name: 'email', label: 'Email', type: 'email', required: true, placeholder: 'Enter your email'},
  textarea: {name: 'message', label: 'Message', type: 'textarea', required: false, placeholder: 'Enter your message'},
  date: {name: 'birthDate', label: 'Birth Date', type: 'date', required: false},
  select: {
    name: 'country',
    label: 'Country',
    type: 'select',
    required: true,
    options: [
      {value: 'us', label: 'United States'},
      {value: 'ca', label: 'Canada'},
    ],
  },
  radio: {
    name: 'gender',
    label: 'Gender',
    type: 'radio',
    required: false,
    options: [
      {value: 'male', label: 'Male'},
      {value: 'female', label: 'Female'},
    ],
  },
  checkbox: {name: 'newsletter', label: 'Subscribe to Newsletter', type: 'checkbox', required: false},
  checkboxGroup: {
    name: 'interests',
    label: 'Interests',
    type: 'checkbox',
    required: false,
    options: [
      {value: 'coding', label: 'Coding'},
      {value: 'music', label: 'Music'},
    ],
  },
} satisfies Record<string, FormField>;
