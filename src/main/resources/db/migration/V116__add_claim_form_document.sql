ALTER TABLE claim
  ADD COLUMN claim_form_document_id UUID REFERENCES document(id);
