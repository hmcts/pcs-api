ALTER TABLE claim
  ADD COLUMN claim_form_document_id UUID REFERENCES document(id);

CREATE INDEX idx_claim_claim_form_document_id ON claim (claim_form_document_id);
