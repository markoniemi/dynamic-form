import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {ReadOnlyDynamicForm} from '../../src/components/ReadOnlyDynamicForm';
import {FormField} from '../../src/types/Form';

describe('ReadOnlyDynamicForm Component', () => {
  const textField: FormField = {
    name: 'fullName',
    label: 'Full Name',
    type: 'text',
    required: true,
  };

  const selectField: FormField = {
    name: 'country',
    label: 'Country',
    type: 'select',
    required: true,
    options: [
      {value: 'us', label: 'United States'},
      {value: 'ca', label: 'Canada'},
    ],
  };

  const radioField: FormField = {
    name: 'gender',
    label: 'Gender',
    type: 'radio',
    required: false,
    options: [
      {value: 'male', label: 'Male'},
      {value: 'female', label: 'Female'},
    ],
  };

  const checkboxField: FormField = {
    name: 'newsletter',
    label: 'Subscribe to Newsletter',
    type: 'checkbox',
    required: false,
  };

  const dateField: FormField = {
    name: 'birthDate',
    label: 'Birth Date',
    type: 'date',
    required: false,
  };

  it('renders text field with value', () => {
    render(<ReadOnlyDynamicForm fields={[textField]} data={{fullName: 'John Doe'}}/>);

    expect(screen.getByText('Full Name')).toBeInTheDocument();
    expect(screen.getByDisplayValue('John Doe')).toBeInTheDocument();
  });

  it('renders dash for empty values', () => {
    render(<ReadOnlyDynamicForm fields={[textField]} data={{}}/>);

    expect(screen.getByDisplayValue('—')).toBeInTheDocument();
  });

  it('renders select field with label instead of value', () => {
    render(<ReadOnlyDynamicForm fields={[selectField]} data={{country: 'us'}}/>);

    expect(screen.getByText('Country')).toBeInTheDocument();
    expect(screen.getByDisplayValue('United States')).toBeInTheDocument();
  });

  it('renders radio field with label instead of value', () => {
    render(<ReadOnlyDynamicForm fields={[radioField]} data={{gender: 'female'}}/>);

    expect(screen.getByText('Gender')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Female')).toBeInTheDocument();
  });

  it('renders checkbox field as "Yes" when true', () => {
    render(<ReadOnlyDynamicForm fields={[checkboxField]} data={{newsletter: true}}/>);

    expect(screen.getByDisplayValue('Yes')).toBeInTheDocument();
  });

  it('renders checkbox field as "No" when false', () => {
    render(<ReadOnlyDynamicForm fields={[checkboxField]} data={{newsletter: false}}/>);

    expect(screen.getByDisplayValue('No')).toBeInTheDocument();
  });

  it('renders date field formatted', () => {
    render(<ReadOnlyDynamicForm fields={[dateField]} data={{birthDate: '1990-05-15'}}/>);

    expect(screen.getByText('Birth Date')).toBeInTheDocument();
    // The formatted date depends on locale, just check it's not empty
    const input = screen.getByRole('textbox');
    expect(input).toHaveValue();
    expect(input).not.toHaveValue('—');
  });

  it('renders multiple fields', () => {
    render(
      <ReadOnlyDynamicForm
        fields={[textField, selectField, checkboxField]}
        data={{fullName: 'Jane', country: 'ca', newsletter: true}}
      />
    );

    expect(screen.getByDisplayValue('Jane')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Canada')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Yes')).toBeInTheDocument();
  });

  it('falls back to raw value when option not found', () => {
    render(<ReadOnlyDynamicForm fields={[selectField]} data={{country: 'unknown'}}/>);

    expect(screen.getByDisplayValue('unknown')).toBeInTheDocument();
  });

  it('does not show required asterisks in read-only mode', () => {
    render(<ReadOnlyDynamicForm fields={[textField]} data={{fullName: 'Test'}}/>);

    expect(screen.queryByText('*')).not.toBeInTheDocument();
  });
});

