-- Move GA rank uniqueness from party-level to case-level.
-- Drop the old constraint that included party_id.
ALTER TABLE general_application
    DROP CONSTRAINT IF EXISTS uq_case_party_rank;

-- Add the new case-level unique constraint.
ALTER TABLE general_application
    ADD CONSTRAINT uq_general_application_case_rank UNIQUE (case_id, rank);
