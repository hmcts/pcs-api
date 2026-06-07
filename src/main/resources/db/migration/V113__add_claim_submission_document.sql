ALTER TABLE claim
  ADD COLUMN submission_document_id UUID REFERENCES document(id);
