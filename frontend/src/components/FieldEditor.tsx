import React from 'react';
import {Button, Card, Col, Form, Row} from 'react-bootstrap';
import {useTranslation} from 'react-i18next';
import {FieldOption, FormField} from '../types/Form';

interface FieldTypeOption {
  value: string;
  label: string;
}

interface FieldEditorProps {
  field: FormField;
  index: number;
  totalFields: number;
  fieldTypes: readonly FieldTypeOption[];
  onChange: (field: FormField) => void;
  onRemove: () => void;
  onMove: (direction: 'up' | 'down') => void;
}

export const FieldEditor: React.FC<FieldEditorProps> = ({
                                                          field,
                                                          index,
                                                          totalFields,
                                                          fieldTypes,
                                                          onChange,
                                                          onRemove,
                                                          onMove,
                                                        }) => {
  const {t} = useTranslation();
  const needsOptions = field.type === 'select' || field.type === 'radio';

  const handleChange = (key: keyof FormField, value: string | boolean) => {
    const updated = {...field, [key]: value};

    // Reset options when switching to/from types that need them
    if (key === 'type') {
      const newNeedsOptions = value === 'select' || value === 'radio';
      if (newNeedsOptions && (!field.options || field.options.length === 0)) {
        updated.options = [{value: '', label: ''}];
      } else if (!newNeedsOptions) {
        updated.options = [];
      }
    }

    onChange(updated);
  };

  const handleOptionChange = (optionIndex: number, key: keyof FieldOption, value: string) => {
    const newOptions = [...(field.options || [])];
    newOptions[optionIndex] = {...newOptions[optionIndex], [key]: value};
    onChange({...field, options: newOptions});
  };

  const handleAddOption = () => {
    const newOptions = [...(field.options || []), {value: '', label: ''}];
    onChange({...field, options: newOptions});
  };

  const handleRemoveOption = (optionIndex: number) => {
    if (field.options && field.options.length > 1) {
      const newOptions = field.options.filter((_, i) => i !== optionIndex);
      onChange({...field, options: newOptions});
    }
  };

  return (
    <Card className="mb-3 border-secondary">
      <Card.Header className="d-flex justify-content-between align-items-center bg-light">
        <span>{t('fieldEditor.fieldHeader', {number: index + 1})}</span>
        <div className="d-flex gap-1">
          <Button
            variant="outline-secondary"
            size="sm"
            onClick={() => onMove('up')}
            disabled={index === 0}
            title={t('fieldEditor.moveUp')}
          >
            ↑
          </Button>
          <Button
            variant="outline-secondary"
            size="sm"
            onClick={() => onMove('down')}
            disabled={index === totalFields - 1}
            title={t('fieldEditor.moveDown')}
          >
            ↓
          </Button>
          <Button
            variant="outline-danger"
            size="sm"
            onClick={onRemove}
            disabled={totalFields === 1}
            title={t('fieldEditor.removeField')}
          >
            ×
          </Button>
        </div>
      </Card.Header>
      <Card.Body>
        <Row>
          <Col md={4}>
            <Form.Group className="mb-3" controlId={`field-${index}-name`}>
              <Form.Label>{t('fieldEditor.fieldName')} <span className="text-danger">*</span></Form.Label>
              <Form.Control
                type="text"
                placeholder="e.g., fullName"
                value={field.name}
                onChange={(e) => handleChange('name', e.target.value)}
              />
            </Form.Group>
          </Col>
          <Col md={4}>
            <Form.Group className="mb-3" controlId={`field-${index}-label`}>
              <Form.Label>{t('fieldEditor.label')} <span className="text-danger">*</span></Form.Label>
              <Form.Control
                type="text"
                placeholder="e.g., Full Name"
                value={field.label}
                onChange={(e) => handleChange('label', e.target.value)}
              />
            </Form.Group>
          </Col>
          <Col md={4}>
            <Form.Group className="mb-3" controlId={`field-${index}-type`}>
              <Form.Label>{t('fieldEditor.type')} <span className="text-danger">*</span></Form.Label>
              <Form.Select
                value={field.type}
                onChange={(e) => handleChange('type', e.target.value)}
              >
                {fieldTypes.map((type) => (
                  <option key={type.value} value={type.value}>
                    {type.label}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
          </Col>
        </Row>

        <Row>
          <Col md={6}>
            <Form.Group className="mb-3" controlId={`field-${index}-placeholder`}>
              <Form.Label>{t('fieldEditor.placeholder')}</Form.Label>
              <Form.Control
                type="text"
                placeholder={t('fieldEditor.placeholderHint')}
                value={field.placeholder || ''}
                onChange={(e) => handleChange('placeholder', e.target.value)}
              />
            </Form.Group>
          </Col>
          <Col md={6} className="d-flex align-items-center">
            <Form.Check
              type="checkbox"
              id={`field-${index}-required`}
              label={t('fieldEditor.required')}
              checked={field.required}
              onChange={(e) => handleChange('required', e.target.checked)}
            />
          </Col>
        </Row>

        {needsOptions && (
          <div className="mt-3">
            <h6>{t('fieldEditor.options')}</h6>
            {(field.options || []).map((option, optionIndex) => (
              <Row key={optionIndex} className="mb-2 align-items-center">
                <Col md={5}>
                  <Form.Control
                    type="text"
                    placeholder={t('fieldEditor.optionValue')}
                    value={option.value}
                    onChange={(e) => handleOptionChange(optionIndex, 'value', e.target.value)}
                  />
                </Col>
                <Col md={5}>
                  <Form.Control
                    type="text"
                    placeholder={t('fieldEditor.optionLabel')}
                    value={option.label}
                    onChange={(e) => handleOptionChange(optionIndex, 'label', e.target.value)}
                  />
                </Col>
                <Col md={2}>
                  <Button
                    variant="outline-danger"
                    size="sm"
                    onClick={() => handleRemoveOption(optionIndex)}
                    disabled={(field.options?.length || 0) <= 1}
                  >
                    ×
                  </Button>
                </Col>
              </Row>
            ))}
            <Button variant="outline-secondary" size="sm" onClick={handleAddOption}>
              {t('fieldEditor.addOption')}
            </Button>
          </div>
        )}
      </Card.Body>
    </Card>
  );
};

