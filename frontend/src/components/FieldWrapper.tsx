import React from 'react';
import {Form} from 'react-bootstrap';

interface FieldWrapperProps {
  label: string;
  required: boolean;
  children: React.ReactNode;
  controlId?: string;
}

export const FieldWrapper: React.FC<FieldWrapperProps> = ({
                                                            label,
                                                            required,
                                                            children,
                                                            controlId
                                                          }) => (
  <Form.Group className="mb-3" controlId={controlId}>
    <Form.Label>
      {label}
      {required && <span className="text-danger"> *</span>}
    </Form.Label>
    {children}
  </Form.Group>
);
