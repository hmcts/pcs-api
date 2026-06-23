ALTER TABLE case_note
  ADD COLUMN case_id UUID REFERENCES pcs_case (id),
  ALTER COLUMN claim_id DROP NOT NULL;
