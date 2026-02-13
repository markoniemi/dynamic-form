import {render, screen} from '@testing-library/react';
import {DynamicForm} from '../../src/components/DynamicForm';
import {FormField} from '../../src/types/Form';
import {describe, expect, it, vi} from 'vitest';
import {UseFormRegister} from 'react-hook-form';

const mockRegister = vi.fn((name) => ({
    name,
    onChange: vi.fn(),
    onBlur: vi.fn(),
    ref: vi.fn(),
})) as unknown as UseFormRegister<any>;

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
        const fields: FormField[] = [
            {
                name: 'username',
                label: 'Username',
                type: 'text',
                required: true,
                placeholder: 'Enter username',
            },
        ];

        renderDynamicForm(fields);

        expect(screen.getByLabelText(/Username/i)).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Enter username')).toBeInTheDocument();
        expect(screen.getByRole('textbox')).toHaveAttribute('type', 'text');
    });

    it('renders select input correctly', () => {
        const fields: FormField[] = [
            {
                name: 'role',
                label: 'Role',
                type: 'select',
                required: true,
                options: [
                    {value: 'admin', label: 'Admin'},
                    {value: 'user', label: 'User'},
                ],
            },
        ];

        renderDynamicForm(fields);

        expect(screen.getByLabelText(/Role/i)).toBeInTheDocument();
        expect(screen.getByRole('combobox')).toBeInTheDocument();
        expect(screen.getByText('Admin')).toBeInTheDocument();
        expect(screen.getByText('User')).toBeInTheDocument();
    });

    it('renders textarea correctly', () => {
        const fields: FormField[] = [
            {
                name: 'description',
                label: 'Description',
                type: 'textarea',
                required: false,
                placeholder: 'Enter description',
            },
        ];

        renderDynamicForm(fields);

        expect(screen.getByLabelText(/Description/i)).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Enter description')).toBeInTheDocument();
        expect(screen.getByRole('textbox')).toBeInTheDocument();
    });

    it('renders radio buttons correctly', () => {
        const fields: FormField[] = [
            {
                name: 'gender',
                label: 'Gender',
                type: 'radio',
                required: true,
                options: [
                    {value: 'male', label: 'Male'},
                    {value: 'female', label: 'Female'},
                ],
            },
        ];

        renderDynamicForm(fields);

        expect(screen.getByLabelText('Male')).toBeInTheDocument();
        expect(screen.getByLabelText('Female')).toBeInTheDocument();
        const radios = screen.getAllByRole('radio');
        expect(radios).toHaveLength(2);
    });

    it('renders checkboxes correctly', () => {
        const fields: FormField[] = [
            {
                name: 'interests',
                label: 'Interests',
                type: 'checkbox',
                required: false,
                options: [
                    {value: 'coding', label: 'Coding'},
                    {value: 'music', label: 'Music'},
                ],
            },
        ];

        renderDynamicForm(fields);

        expect(screen.getByLabelText('Coding')).toBeInTheDocument();
        expect(screen.getByLabelText('Music')).toBeInTheDocument();
        const checkboxes = screen.getAllByRole('checkbox');
        expect(checkboxes).toHaveLength(2);
    });

    it('displays error message when present', () => {
        const fields: FormField[] = [
            {
                name: 'username',
                label: 'Username',
                type: 'text',
                required: true,
            },
        ];

        const errors = {
            username: {
                type: 'required',
                message: 'Username is required',
            },
        };

        renderDynamicForm(fields, errors);

        expect(screen.getByText('Username is required')).toBeInTheDocument();
        expect(screen.getByRole('textbox')).toHaveClass('is-invalid');
    });
});
