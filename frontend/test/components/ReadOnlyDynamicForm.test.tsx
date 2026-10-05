import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {ReadOnlyDynamicForm} from '../../src/components/ReadOnlyDynamicForm';
import {TestFields} from '../testdata/TestFields';

describe('ReadOnlyDynamicForm Component', () => {
  const {text, select, radio, checkbox, date} = TestFields;

  it('renders text field with value', () => {
    render(<ReadOnlyDynamicForm fields={[text]} data={{[text.name]: 'John Doe'}}/>);

    expect(screen.getByText(text.label)).toBeInTheDocument();
    expect(screen.getByDisplayValue('John Doe')).toBeInTheDocument();
  });

  it('renders dash for empty values', () => {
    render(<ReadOnlyDynamicForm fields={[text]} data={{}}/>);

    expect(screen.getByDisplayValue('common.empty')).toBeInTheDocument();
  });

  it('renders select field with label instead of value', () => {
    const [option] = select.options;
    render(<ReadOnlyDynamicForm fields={[select]} data={{[select.name]: option.value}}/>);

    expect(screen.getByText(select.label)).toBeInTheDocument();
    expect(screen.getByDisplayValue(option.label)).toBeInTheDocument();
  });

  it('renders radio field with label instead of value', () => {
    const [, option] = radio.options;
    render(<ReadOnlyDynamicForm fields={[radio]} data={{[radio.name]: option.value}}/>);

    expect(screen.getByText(radio.label)).toBeInTheDocument();
    expect(screen.getByDisplayValue(option.label)).toBeInTheDocument();
  });

  it('renders checkbox field as "Yes" when true', () => {
    render(<ReadOnlyDynamicForm fields={[checkbox]} data={{[checkbox.name]: true}}/>);

    expect(screen.getByDisplayValue('common.yes')).toBeInTheDocument();
  });

  it('renders checkbox field as "No" when false', () => {
    render(<ReadOnlyDynamicForm fields={[checkbox]} data={{[checkbox.name]: false}}/>);

    expect(screen.getByDisplayValue('common.no')).toBeInTheDocument();
  });

  it('renders date field formatted', () => {
    render(<ReadOnlyDynamicForm fields={[date]} data={{[date.name]: '1990-05-15'}}/>);

    expect(screen.getByText(date.label)).toBeInTheDocument();
    // The formatted date depends on locale, just check it's not empty
    const input = screen.getByRole('textbox');
    expect(input).toHaveValue();
    expect(input).not.toHaveValue('—');
  });

  it('renders multiple fields', () => {
    const [, option] = select.options;
    render(
      <ReadOnlyDynamicForm
        fields={[text, select, checkbox]}
        data={{[text.name]: 'Jane', [select.name]: option.value, [checkbox.name]: true}}
      />
    );

    expect(screen.getByDisplayValue('Jane')).toBeInTheDocument();
    expect(screen.getByDisplayValue(option.label)).toBeInTheDocument();
    expect(screen.getByDisplayValue('common.yes')).toBeInTheDocument();
  });

  it('falls back to raw value when option not found', () => {
    render(<ReadOnlyDynamicForm fields={[select]} data={{[select.name]: 'unknown'}}/>);

    expect(screen.getByDisplayValue('unknown')).toBeInTheDocument();
  });

  it('does not show required asterisks in read-only mode', () => {
    render(<ReadOnlyDynamicForm fields={[text]} data={{[text.name]: 'Test'}}/>);

    expect(screen.queryByText('*')).not.toBeInTheDocument();
  });
});
