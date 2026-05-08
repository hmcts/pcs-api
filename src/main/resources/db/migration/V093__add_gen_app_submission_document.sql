ALTER TABLE general_application
  ADD COLUMN submission_document_id UUID REFERENCES document(id);

ALTER TABLE pcs.public.document
  ADD COLUMN document_id UUID;


