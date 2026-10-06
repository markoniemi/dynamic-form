import {describe, expect, it, vi} from 'vitest';
import type {TFunction} from 'i18next';
import {requiredRule} from '../../src/components/FieldProps';
import {TestFields} from '../testdata/TestFields';

const t = vi.fn((key: string, options?: {label?: string}) => `${key}:${options?.label}`) as unknown as TFunction;

describe('requiredRule', () => {
  it('returns translated message with field label for required field', () => {
    const field = TestFields.text;

    expect(requiredRule(field, t)).toEqual({required: `form.required:${field.label}`});
    expect(t).toHaveBeenCalledWith('form.required', {label: field.label});
  });

  it('returns required false for optional field', () => {
    expect(requiredRule(TestFields.textarea, t)).toEqual({required: false});
  });
});
