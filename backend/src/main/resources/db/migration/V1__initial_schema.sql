-- Create form table
CREATE TABLE form (
  id BIGSERIAL PRIMARY KEY,
  form_key VARCHAR(255) NOT NULL UNIQUE,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  fields JSONB NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create form_data table
CREATE TABLE form_data (
  id BIGSERIAL PRIMARY KEY,
  form_key VARCHAR(255) NOT NULL,
  data JSONB NOT NULL,
  submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  submitted_by VARCHAR(255) NOT NULL
);

-- Create indexes for commonly queried columns
CREATE INDEX idx_form_data_form_key ON form_data(form_key);
CREATE INDEX idx_form_data_submitted_at ON form_data(submitted_at);
