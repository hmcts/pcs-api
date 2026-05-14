ALTER TABLE party_attribute_assertion
  RENAME COLUMN decided_at TO last_updated_at;

ALTER TABLE party_attribute_assertion
  ALTER COLUMN evidence_document_id DROP NOT NULL; 

ALTER TABLE party_attribute_assertion
  ADD COLUMN created_by UUID NOT NULL REFERENCES party(id),
  ADD COLUMN last_updated_by UUID NOT NULL REFERENCES party(id);
