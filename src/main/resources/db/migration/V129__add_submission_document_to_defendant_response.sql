ALTER TABLE defendant_response
  ADD COLUMN submission_document_id UUID REFERENCES document(id) ON DELETE SET NULL;

CREATE INDEX idx_defendant_response_submission_document_id
  ON defendant_response (submission_document_id);
