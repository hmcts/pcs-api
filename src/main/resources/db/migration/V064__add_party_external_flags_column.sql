ALTER TABLE pcs_case
  ADD COLUMN IF NOT EXISTS case_flags jsonb;

ALTER TABLE party
  ADD COLUMN IF NOT EXISTS flags jsonb;

ALTER TABLE party
    ADD COLUMN IF NOT EXISTS external_flags jsonb;
