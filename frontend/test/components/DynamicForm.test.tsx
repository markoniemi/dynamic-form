import {render, screen} from '@testing-library/react';
import {DynamicForm} from '../../src/components/DynamicForm';
import {FormField, FormValues} from '../../src/types/Form';
import {describe, expect, it, vi} from 'vitest';
import {UseFormRegister} from 'react-hook-form';
import {TestFields} from '../testdata/TestFields';

// Mock; only the subset of register API exercised by DynamicForm is needed (name, onChange, onBlur, ref)
const mockRegister = vi.fn((name) => ({
  name,
  onChange: vi.fn(),
  onBlur: vi.fn(),
  ref: vi.fn(),
})) as unknown as UseFormRegister<FormValues>;

const mockErrors = {};

function renderDynamicForm(fields: FormField[], errors = mockErrors) {
  render(
    <DynamicForm
      fields={fields}
      register={mockRegister}
      errors={errors}
    />
  );
}

describe('DynamicForm', () => {

  it('renders text input correctly', () => {
    const field = TestFields.text;
    renderDynamicForm([field]);

    expect(screen.getByLabelText(field.label, {exact: false})).toBeInTheDocument();
    expect(screen.getByPlaceholderText(field.placeholder)).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toHaveAttribute('type', 'text');
  });

  it('renders select input correctly', () => {
    const field = TestFields.select;
    renderDynamicForm([field]);

    expect(screen.getByLabelText(field.label, {exact: false})).toBeInTheDocument();
    expect(screen.getByRole('combobox')).toBeInTheDocument();
    field.options.forEach(({label}) => expect(screen.getByText(label)).toBeInTheDocument());
  });

  it('renders textarea correctly', () => {
    const field = TestFields.textarea;
    renderDynamicForm([field]);

    expect(screen.getByLabelText(field.label, {exact: false})).toBeInTheDocument();
    expect(screen.getByPlaceholderText(field.placeholder)).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toBeInTheDocument();
  });

  it('renders radio buttons correctly', () => {
    const field = TestFields.radio;
    renderDynamicForm([field]);

    field.options.forEach(({label}) => expect(screen.getByLabelText(label)).toBeInTheDocument());
    expect(screen.getAllByRole('radio')).toHaveLength(field.options.length);
  });

  it('renders checkboxes correctly', () => {
    const field = TestFields.checkboxGroup;
    renderDynamicForm([field]);

    field.options.forEach(({label}) => expect(screen.getByLabelText(label)).toBeInTheDocument());
    expect(screen.getAllByRole('checkbox')).toHaveLength(field.options.length);
  });

  it('displays error message when present', () => {
    const field = TestFields.text;
    const errors = {
      [field.name]: {
        type: 'required',
        message: 'Full name is required',
      },
    };

    renderDynamicForm([field], errors);

    expect(screen.getByText('Full name is required')).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toHaveClass('is-invalid');
  });
});
