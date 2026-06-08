ALTER TABLE claim
  ADD COLUMN claim_pack_document_id UUID REFERENCES document(id);
