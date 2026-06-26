-- Move GA rank uniqueness from party-level to case-level.
-- Drop the old constraint that included party_id.
ALTER TABLE general_application
    DROP CONSTRAINT IF EXISTS uq_case_party_rank;

-- Re-number any existing rows so that the first GA per case is rank 1, the second is rank 2, etc.
WITH renumbered AS (
  SELECT id,
         ROW_NUMBER() OVER (PARTITION BY case_id ORDER BY application_submitted_date NULLS LAST, id) AS new_rank
  FROM general_application
)
UPDATE general_application ga
SET rank = r.new_rank
  FROM renumbered r
WHERE ga.id = r.id;

-- Add the new case-level unique constraint.
ALTER TABLE general_application
    ADD CONSTRAINT uq_general_application_case_rank UNIQUE (case_id, rank);
