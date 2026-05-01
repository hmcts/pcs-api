ALTER TABLE document
  ADD COLUMN content_type VARCHAR(200),
  ADD COLUMN size BIGINT,
  ADD COLUMN display_file_name TEXT,
  ADD COLUMN general_application_id UUID REFERENCES general_application(id);

CREATE INDEX idx_document_general_application_id ON document(general_application_id);
